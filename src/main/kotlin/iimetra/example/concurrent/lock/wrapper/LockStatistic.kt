package iimetra.example.concurrent.lock.wrapper

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class LockStatistic {
    val requestedThreads = CopyOnWriteArrayList<Long>()
    var ownerThread: Long? = null
        private set
    val version = AtomicLong(0)

    var lastRequestTime = 0L
        private set

    fun visit() {
        version.incrementAndGet()
        val currentThreadId = Thread.currentThread().id
        requestedThreads.remove(currentThreadId)
        ownerThread = currentThreadId
        lastRequestTime = System.currentTimeMillis()
    }

    fun leave() {
        version.incrementAndGet()
        ownerThread = null
    }

    fun request() {
        version.incrementAndGet()
        requestedThreads.add(Thread.currentThread().id)
    }
}