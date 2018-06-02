package iimetra.example.concurrent.lock.strategy

import iimetra.example.concurrent.lock.wrapper.LockWrapper
import java.util.concurrent.ConcurrentHashMap

class DeadLockStrategyExecutor(repeatPeriod: Long, private val lockMap: ConcurrentHashMap<Any, LockWrapper>) : StrategyExecutor(repeatPeriod) {

    override fun process() {
        var firstAttempt = getDeadLockWrappers()
        if (firstAttempt.isNotEmpty()) {
            var tmp = getDeadLockWrappers()
            while (tmp.isNotEmpty()) {
                if (equalMaps(firstAttempt, tmp)) {
                    throw DeadLockException()
                }
                firstAttempt = tmp
                tmp = getDeadLockWrappers()
            }
        }
    }

    private fun getDeadLockWrappers(): Map<String, Long> {
        val graph = DeadlockGraph()
        lockMap.values.forEach {
            graph.addData(it)
        }
        return graph.check()
    }

    private fun equalMaps(first: Map<String, Long>, second: Map<String, Long>) =
        first.entries.all { second.entries.contains(it) }
                && second.entries.all { first.entries.contains(it) }

}

class DeadlockGraph {
    private val requestMap = mutableMapOf<Long, MutableList<String>>()
    private val ownershipMap = mutableMapOf<String, Long>()
    private val versionMap = mutableMapOf<String, Long>()

    fun addData(wrapper: LockWrapper) {
        val ownerThreadId = wrapper.lockStatistic.ownerThread
        if (ownerThreadId != null) {
            requestMap.computeIfAbsent(ownerThreadId) { mutableListOf() }.add(wrapper.uid)
            ownershipMap[wrapper.uid] = ownerThreadId
            versionMap[wrapper.uid] = wrapper.lockStatistic.version.get()
        }
    }

    fun check(): Map<String, Long> {
        val previous = mutableMapOf<String, String>()
        val deadlockListOptional = ownershipMap.keys.stream()
            .filter { previous[it] == null }
            .map { checkInner(it, previous) }
            .filter { it.isNotEmpty() }
            .findFirst()

        if (!deadlockListOptional.isPresent) {
            return emptyMap()
        }

        val deadLockList = deadlockListOptional.get()

        return deadLockList.map { it to (versionMap[it] ?: throw IllegalStateException("Each wrapper has a vertex")) }.toMap()
    }

    private fun checkInner(wrapperId: String, previous: MutableMap<String, String>): List<String> {
        val ownerThread = ownershipMap[wrapperId] ?: return emptyList()
        val nextWrapperList = requestMap.getOrDefault(ownerThread, emptyList<String>())
        nextWrapperList.find { previous[it] != null }?.let {
            previous[it] = wrapperId
            return findCycle(it, previous)
        }
        nextWrapperList.forEach { previous[it] = wrapperId }
        return nextWrapperList.map { checkInner(it, previous) }.first { it.isNotEmpty() }
    }

    private fun findCycle(lastId: String, previous: MutableMap<String, String>): List<String> {
        val result = mutableListOf<String>()
        var tmp = lastId
        while (previous[tmp] != lastId) {
            result.add(tmp)
            tmp = previous[tmp] ?: throw IllegalStateException("Cycle must exist")
        }
        return result
    }
}

class DeadLockException : Exception()