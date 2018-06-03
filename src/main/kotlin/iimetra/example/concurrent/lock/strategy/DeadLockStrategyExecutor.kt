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
                    throw DeadLockException("Threads in block: " + tmp.keys.joinToString(", "))
                }
                firstAttempt = tmp
                tmp = getDeadLockWrappers()
            }
        }
    }

    private fun getDeadLockWrappers(): Map<String, Int> {
        val graph = DeadlockGraph()
        lockMap.values.forEach {
            graph.addData(it)
        }
        return graph.check()
    }

    private fun equalMaps(first: Map<String, Int>, second: Map<String, Int>) =
        first.entries.all { second.entries.contains(it) }
                && second.entries.all { first.entries.contains(it) }

}

class DeadlockGraph {
    private val requestMap = mutableMapOf<Long, MutableSet<String>>()
    private val ownershipMap = mutableMapOf<String, Long>()
    private val versionMap = mutableMapOf<String, Int>()

    fun addData(wrapper: LockWrapper) {
        val ownerThreadId = wrapper.lockStatistic.ownerThread?.id
        if (ownerThreadId != null) {
            ownershipMap[wrapper.uid] = ownerThreadId
        }
        val threadsAndVersion = wrapper.lockStatistic.threadsAndVersion()
        versionMap[wrapper.uid] = threadsAndVersion.second

        threadsAndVersion.first.forEach {
            requestMap.computeIfAbsent(it.id) { mutableSetOf() }.add(wrapper.uid)
        }
    }

    fun check(): Map<String, Int> {
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
        nextWrapperList.find { it != wrapperId && previous.values.contains(it) }?.let {
            previous[it] = wrapperId
            return findCycle(it, previous)
        }
        nextWrapperList.forEach { previous[it] = wrapperId }
        return nextWrapperList.map { checkInner(it, previous) }.firstOrNull { it.isNotEmpty() } ?: emptyList()
    }

    private fun findCycle(lastId: String, previous: MutableMap<String, String>): List<String> {
        val result = mutableListOf<String>()
        var tmp = lastId
        while (previous[tmp] != lastId) {
            result.add(tmp)
            tmp = previous[tmp] ?: throw IllegalStateException("Cycle must exist")
        }
        result.add(tmp)
        return result
    }
}

class DeadLockException(message: String) : RuntimeException(message)