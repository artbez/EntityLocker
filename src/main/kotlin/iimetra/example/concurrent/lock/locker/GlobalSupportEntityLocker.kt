package iimetra.example.concurrent.lock.locker

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


interface GlobalSupportEntityLocker : EntityLocker {
    fun lock()
    fun unlock()
    fun tryLock(): Boolean
}

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
    private val glLockNotProcessingCond = globalLock.newCondition()
    private val unlockLocalCond = globalLock.newCondition()
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