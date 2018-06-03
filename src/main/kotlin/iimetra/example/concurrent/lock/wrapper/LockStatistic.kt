package iimetra.example.concurrent.lock.wrapper

import java.util.concurrent.atomic.AtomicStampedReference

class LockStatistic {
    private val requestedThreadsAndVersion = AtomicStampedReference<Set<Thread>>(HashSet(), 0)

    var ownerThread: Thread? = null
        private set

    var lastRequestTime = 0L
        private set

    // executes in lock section
    fun visit() {
        val currentThreadId = Thread.currentThread()
        update { it.minus(currentThreadId) }
        ownerThread = currentThreadId
        lastRequestTime = System.currentTimeMillis()
    }

    // executes with in lock section
    fun leave() {
        requestedThreadsAndVersion.set(requestedThreadsAndVersion.reference, requestedThreadsAndVersion.stamp + 1)
        ownerThread = null
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