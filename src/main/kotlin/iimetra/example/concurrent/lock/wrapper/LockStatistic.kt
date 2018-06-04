package iimetra.example.concurrent.lock.wrapper

import java.util.concurrent.atomic.AtomicStampedReference

/**
 * Statistic for [LockWrapper].
 */
class LockStatistic {
    // set of threads that wait for lock and Statistic version
    private val requestedThreadsAndVersion = AtomicStampedReference<Set<Thread>>(HashSet(), 0)

    // Thread that owns lock now
    var ownerThread: Thread? = null
        private set

    // Last time a thread owns lock
    var lastOwningTime = 0L
        private set

    // Executes in lock section
    fun visit() {
        val currentThread = Thread.currentThread()
        update { it.minus(currentThread) }
        ownerThread = currentThread
        lastOwningTime = System.currentTimeMillis()
    }

    // Executes with in lock section
    fun leave() {
        ownerThread = null
        update { it }
    }

    fun request() {
        update { it.plus(Thread.currentThread()) }
    }

    fun threadsAndVersion(): Pair<Set<Thread>, Int> {
        while (true) {
            val requestedSet = requestedThreadsAndVersion.reference
            val lastVersion = requestedThreadsAndVersion.stamp
            if (requestedThreadsAndVersion.compareAndSet(requestedSet, requestedSet, lastVersion, lastVersion)) {
                return requestedSet to lastVersion
            }
        }
    }

    // Update set<thread> to next version with set transform.
    private fun update(listUpdate: (Set<Thread>) -> Set<Thread>) {
        while (true) {
            val requestedList = requestedThreadsAndVersion.reference
            val lastVersion = requestedThreadsAndVersion.stamp
            val newList = listUpdate(requestedList)
            if (requestedThreadsAndVersion.compareAndSet(requestedList, newList, lastVersion, lastVersion + 1)) {
                break
            }
        }
    }
}