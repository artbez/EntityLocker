package iimetra.example.concurrent.lock.wrapper

/**
 * Additional info about lock access for [LockWrapper].
 */
class LockOwningInfo {
    // Set of threads that wait for lock. Need for deadlock resolving
    private val requestedThreads: MutableSet<Thread> = HashSet()
    // Info version
    private var version: Int = 0

    // Thread that owns lock now.
    var ownerThread: Thread? = null
        private set

    // Last time a thread owns lock.
    var lastOwningTime = 0L
        private set

    // Thread got the lock.
    @Synchronized
    fun receivedLock() {
        val currentThread = Thread.currentThread()

        version++
        requestedThreads.remove(currentThread)
        ownerThread = currentThread
        lastOwningTime = System.currentTimeMillis()
    }

    // Thread released the lock.
    @Synchronized
    fun releasedLock() {
        ownerThread = null
        version++
    }

    // Thread waiting the lock.
    @Synchronized
    fun requestLock() {
        version++
        requestedThreads.add(Thread.currentThread())
    }

    @Synchronized
    fun waitingThreadsAndInfoVersion(): Pair<Set<Thread>, Int> = requestedThreads.toSet() to version
}