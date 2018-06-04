package iimetra.example.concurrent.lock.strategy

import iimetra.example.concurrent.lock.wrapper.LockWrapper
import java.util.concurrent.ConcurrentHashMap

/**
 * Dead lock searching [StrategyExecutor].
 */
class DeadLockStrategyExecutor(repeatPeriod: Long, private val lockMap: ConcurrentHashMap<Any, LockWrapper>) : StrategyExecutor(repeatPeriod) {

    /**
     * @throws DeadLockException in case deadlock was found.
     */
    override fun process() {

        var baseDeadlockSet = getDeadLockWrappers()
        if (baseDeadlockSet.isNotEmpty()) {
            // We suppose that if same systems state (with versions) occurs two times that state is correct.
            var actualDeadlockSet = getDeadLockWrappers()
            while (actualDeadlockSet.isNotEmpty()) {

                if (equalMaps(baseDeadlockSet, actualDeadlockSet)) {
                    throw DeadLockException("Threads in block: " + actualDeadlockSet.keys.joinToString(", "))
                }

                baseDeadlockSet = actualDeadlockSet
                actualDeadlockSet = getDeadLockWrappers()
            }
        }
    }

    private fun getDeadLockWrappers(): Map<String, Int> {
        val graph = DeadlockGraph()

        lockMap.values.forEach {
            graph.addData(it)
        }
        return graph.findDeadlockWrappers()
    }

    private fun equalMaps(first: Map<String, Int>, second: Map<String, Int>) =
        first.entries.all { second.entries.contains(it) }
                && second.entries.all { first.entries.contains(it) }

}

class DeadlockGraph {
    // threadId <-> set<requesting lock>
    private val requestMap = mutableMapOf<Long, MutableSet<String>>()
    // lockId <-> owning thread
    private val ownershipMap = mutableMapOf<String, Long>()
    // lock <-> version
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

    fun findDeadlockWrappers(): Map<String, Int> {

        val previous = mutableMapOf<String, String>()

        val deadlockListOptional = ownershipMap.keys.stream()
            .filter { previous[it] == null }
            .map { findDeadlock(it, previous) }
            .filter { it.isNotEmpty() }
            .findFirst()

        if (!deadlockListOptional.isPresent) {
            return emptyMap()
        }

        val deadLockList = deadlockListOptional.get()

        return deadLockList.map { it to (versionMap[it] ?: throw IllegalStateException("Each wrapper has a vertex")) }.toMap()
    }

    // traverse until find visited
    private fun findDeadlock(wrapperId: String, previous: MutableMap<String, String>): List<String> {

        val ownerThread = ownershipMap[wrapperId] ?: return emptyList()
        val nextWrapperList = requestMap.getOrDefault(ownerThread, emptyList<String>())

        nextWrapperList.find { it != wrapperId && previous.values.contains(it) }?.let {
            previous[it] = wrapperId
            return findCycle(it, previous)
        }

        nextWrapperList.forEach { previous[it] = wrapperId }
        return nextWrapperList.map { findDeadlock(it, previous) }.firstOrNull { it.isNotEmpty() } ?: emptyList()
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