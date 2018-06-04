package iimetra.example.concurrent.lock.locker

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Global entity locker interface.
 * Provides methods for global locking.
 */
interface GlobalSupportEntityLocker : EntityLocker {

    /**
     * Opens global protected section.
     * Protected code that executes under a global lock must not execute concurrently with any other protected code.
     * Waits until all entities be unlocked.
     *
     * Global locking is not reentrant.
     * Global locking can't be called from local locking section.
     * In these cases deadlock occurs.
     * */
    fun lock()

    /** Closes global protected section. */
    fun unlock()

    /** Try to open global protected section.
     *
     * @return <code>true</code> if success
     *         <code>false</code> if there is a thread that executes global or any local section.
     * */
    fun tryLock(): Boolean
}

/**
 * For a clear selection of the locking code area.
 *
 * locker.globalLock {
 *    someWork
 * }
 * */
inline fun GlobalSupportEntityLocker.globalLock(protectedCode: () -> Unit) {
    lock()
    try {
        protectedCode()
    } finally {
        unlock()
    }
}

class GlobalSupportEntityLockerDecorator(private val locker: EntityLocker) : EntityLocker by (locker), GlobalSupportEntityLocker {

    private val globalLock: Lock = ReentrantLock()
    // signals when no threads execute global locking section
    private val glLockNotProcessingCond = globalLock.newCondition()
    // signals when local unlock called
    private val unlockLocalCond = globalLock.newCondition()
    // number of opened but not closed local locks
    private val localLocksProcessingNumber = AtomicInteger(0)
    private var globalLockProcessing = false

    override fun lock() {
        globalLockBlock {

            while (globalLockProcessing) {
                glLockNotProcessingCond.await()
            }

            globalLockProcessing = true


            while (localLocksProcessingNumber.get() != 0) {
                unlockLocalCond.await()
            }
        }
    }

    override fun unlock() {
        globalLockBlock {
            globalLockProcessing = false
            glLockNotProcessingCond.signalAll()
        }
    }

    override fun tryLock(): Boolean {
        globalLockBlock {
            if (!globalLockProcessing) {
                globalLockProcessing = true
                if (localLocksProcessingNumber.get() == 0) {
                    return true
                }
                globalLockProcessing = false
                glLockNotProcessingCond.signalAll()
            }
        }
        return false
    }

    override fun lock(entityId: Any) {
        globalLockBlock {
            while (globalLockProcessing) {
                glLockNotProcessingCond.await()
            }
            localLocksProcessingNumber.incrementAndGet()
        }
        locker.lock(entityId)
    }

    override fun tryLock(entityId: Any): Boolean {
        globalLockBlock {
            if (!globalLockProcessing) {
                localLocksProcessingNumber.incrementAndGet()
                if (locker.tryLock(entityId)) {
                    return true
                }
                localLocksProcessingNumber.decrementAndGet()
            }
        }
        return false
    }

    override fun unlock(entityId: Any) {
        globalLockBlock {
            locker.unlock(entityId)
            localLocksProcessingNumber.decrementAndGet()
            unlockLocalCond.signalAll()
        }
    }

    private inline fun globalLockBlock(block: () -> Unit) {
        globalLock.lock()
        try {
            block()
        } finally {
            globalLock.unlock()
        }
    }
}