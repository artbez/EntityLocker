package iimetra.example.concurrent.lock.wrapper

/**
 * Statistic for [LockWrapper].
 */
class LockStatistic {
    // Set of threads that wait for lock.
    private val requestedThreads: MutableSet<Thread> = HashSet()
    private var version: Int = 0

    // Thread that owns lock now.
    var ownerThread: Thread? = null
        private set

    // Last time a thread owns lock.
    var lastOwningTime = 0L
        private set

    // Executes in lock section.
    @Synchronized
    fun receivedLock() {
        val currentThread = Thread.currentThread()

        version++
        requestedThreads.remove(currentThread)
        ownerThread = currentThread
        lastOwningTime = System.currentTimeMillis()
    }

    // Executes with in lock section.
    @Synchronized
    fun releasedLock() {
        ownerThread = null
        version++
    }

    @Synchronized
    fun requestLock() {
        version++
        requestedThreads.add(Thread.currentThread())
    }

    @Synchronized
    fun waitingThreadsAndStatVersion(): Pair<Set<Thread>, Int> = requestedThreads.toSet() to version
}