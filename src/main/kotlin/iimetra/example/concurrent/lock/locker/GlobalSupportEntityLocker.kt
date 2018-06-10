package iimetra.example.concurrent.lock.locker

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Global entity locker interface.
 * Provides methods for global locking.
 */
interface GlobalSupportEntityLocker : EntityLocker {

    /**
     * Opens global protected section.
     * Secure code that runs under global locking should not be executed at the same time as any other protected code.
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
    // Signals when no threads execute global locking section.
    private val glLockNotProcessingCond = globalLock.newCondition()
    // Signals when local unlock called
    private val unlockLocalCond = globalLock.newCondition()
    // Number of opened but not closed local locks.
    private var localLocksProcessingNumber = 0
    private var globalLockProcessing = false

    override fun lock() {
        globalLockBlock {

            while (globalLockProcessing) {
                glLockNotProcessingCond.await()
            }

            globalLockProcessing = true

            while (localLocksProcessingNumber != 0) {
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

                if (localLocksProcessingNumber == 0) {
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

            localLocksProcessingNumber++
        }
        locker.lock(entityId)
    }

    override fun tryLock(entityId: Any): Boolean {
        globalLockBlock {
            if (!globalLockProcessing) {
                localLocksProcessingNumber++

                if (locker.tryLock(entityId)) {
                    return true
                }

                localLocksProcessingNumber--
            }
        }
        return false
    }

    override fun unlock(entityId: Any) {
        globalLockBlock {
            locker.unlock(entityId)
            localLocksProcessingNumber--
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