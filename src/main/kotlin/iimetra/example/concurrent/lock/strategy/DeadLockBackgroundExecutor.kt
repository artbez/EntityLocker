package iimetra.example.concurrent.lock.strategy

import iimetra.example.concurrent.lock.wrapper.LockWrapper

/**
 * Dead lock searching [BackgroundExecutor].
 */
class DeadLockBackgroundExecutor(repeatPeriod: Long) : BackgroundExecutor(repeatPeriod) {

    /**
     * @throws DeadLockException in case deadlock was found.
     */
    override fun process(lockMap: MutableMap<Any, LockWrapper>) {

        var baseDeadlockSet = getDeadLockWrappers(lockMap)

        if (baseDeadlockSet.isNotEmpty()) {

            // We suppose that if same systems state (with versions) occurs two times that state is correct.
            var actualDeadlockSet = getDeadLockWrappers(lockMap)

            while (actualDeadlockSet.isNotEmpty()) {

                if (equalMaps(baseDeadlockSet, actualDeadlockSet)) {
                    throw DeadLockException("Threads in block: " + actualDeadlockSet.keys.joinToString(", "))
                }

                baseDeadlockSet = actualDeadlockSet
                actualDeadlockSet = getDeadLockWrappers(lockMap)
            }
        }
    }

    // Returns LockWrappers and their versions which are in deadlock.
    private fun getDeadLockWrappers(lockMap: Map<Any, LockWrapper>): Map<String, Int> {
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
    // ThreadId <-> Set<requesting lockId>.
    private val requestMap = mutableMapOf<Long, MutableSet<String>>()
    // LockId <-> Owning thread.
    private val ownershipMap = mutableMapOf<String, Long>()
    // LockId <-> Version.
    private val versionMap = mutableMapOf<String, Int>()

    fun addData(wrapper: LockWrapper) {
        val ownerThreadId = wrapper.lockStatistic.ownerThread?.id

        if (ownerThreadId != null) {
            ownershipMap[wrapper.uid] = ownerThreadId
        }

        val (waitingThreads, version) = wrapper.lockStatistic.waitingThreadsAndStatVersion()
        versionMap[wrapper.uid] = version

        waitingThreads.forEach {
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

    // Traverse until find visited.
    private fun findDeadlock(wrapperId: String, previous: MutableMap<String, String>): List<String> {

        val ownerThread = ownershipMap[wrapperId] ?: return emptyList()
        val nextWrapperList = requestMap.getOrDefault(ownerThread, emptyList<String>())

        // If wrapper that already was in path (in previous.values) is in nextWrapperList, there is a cycle.
        nextWrapperList.find { it != wrapperId && previous.values.contains(it) }?.let {
            previous[it] = wrapperId
            return findCycle(it, previous)
        }

        nextWrapperList.forEach { previous[it] = wrapperId }

        return nextWrapperList
            .map { findDeadlock(it, previous) }
            .firstOrNull { it.isNotEmpty() } ?: emptyList()
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