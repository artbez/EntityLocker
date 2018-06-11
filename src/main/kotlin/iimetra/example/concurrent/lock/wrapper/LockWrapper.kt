package iimetra.example.concurrent.lock.wrapper

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Wrapper for ReentrantLock.
 *
 * Delegate lock/tryLock/unlock operations to entityIdLock.
 * Provides info about lock access [LockOwningInfo].
 *
 * Can be marked removed.
 */
class LockWrapper {

    // For identifying.
    val uid = UUID.randomUUID().toString()
    // Additional information about access for lock. Used for [BackgroundExecutors].
    val lockOwningInfo = LockOwningInfo()
    // Lock matching entity id.
    private val entityIdLock = ReentrantLock()
    // Inner lock for working inside methods.
    private var innerLock = ReentrantLock()
    // Indicate weather this lockwrapper already deleted.
    private var deleted: Boolean = false
    private var lockConsumersNumber: Int = 0

    fun lock(): Boolean {

        innerLockBlock {
            if (deleted) {
                return false
            }
            lockConsumersNumber++
        }

        lockOwningInfo.requestLock()
        forceLock()
        lockOwningInfo.receivedLock()

        return true
    }

    fun tryLock(): Boolean {

        innerLockBlock {
            if (deleted) {
                return false
            }

            lockConsumersNumber++
            val locked = entityIdLock.tryLock()
            if (locked) {
                lockOwningInfo.receivedLock()
                return true
            }
            lockConsumersNumber--
        }
        return false
    }

    fun unlock() {
        entityIdLock.unlock()
        lockOwningInfo.releasedLock()
        innerLockBlock {
            lockConsumersNumber--
        }
    }

    // If we cannot remove, element is used by another thread and this means we should not remove it.
    fun tryRemove(): Boolean {
        innerLockBlock {
            if (lockConsumersNumber == 0) {
                deleted = true
                return true
            }
        }
        return false
    }

    fun interruptOwningThread() {
        lockOwningInfo.ownerThread?.interrupt()
    }

    private fun forceLock() {
        var locked = entityIdLock.tryLock()
        while (!locked) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt()
                break
            }
            locked = entityIdLock.tryLock(1, TimeUnit.SECONDS)
        }
    }

    private inline fun innerLockBlock(block: () -> Unit) {
        innerLock.lock()
        try {
            block()
        } finally {
            innerLock.unlock()
        }
    }
}