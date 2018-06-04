package iimetra.example.concurrent.lock.locker

import java.util.concurrent.TimeUnit

/**
 * Timeout entity locker interface.
 * Provides a method for timeout locking by entityId.
 */
interface TimeoutEntityLocker : GlobalSupportEntityLocker {
    fun lock(timeout: Long, timeUnit: TimeUnit, entityId: Any): Boolean
}

/**
 * For a clear selection of the locking code area.
 *
 * locker.lock(timeout, timeUnit, id) {
 *    someWork
 * }
 * */
inline fun TimeoutEntityLocker.lock(timeout: Long, timeUnit: TimeUnit, entityId: Any, protectedCode: () -> Unit) {
    val success = lock(timeout, timeUnit, entityId)
    if (success) {
        try {
            protectedCode()
        } finally {
            unlock(entityId)
        }
    }
}

class TimeoutEntityLockerDecorator(private val locker: GlobalSupportEntityLocker) : GlobalSupportEntityLocker by (locker), TimeoutEntityLocker {

    override fun lock(timeout: Long, timeUnit: TimeUnit, entityId: Any): Boolean = repeatTimedRequest(timeout, timeUnit) { tryLock(entityId) }

    // Until the time came do lockRequest
    private inline fun repeatTimedRequest(timeout: Long, timeUnit: TimeUnit, lockRequest: GlobalSupportEntityLocker.() -> Boolean): Boolean {
        val finishTime = System.currentTimeMillis() + timeUnit.toMillis(timeout)
        var successLock = false
        while (!successLock && System.currentTimeMillis() < finishTime) {
            successLock = locker.lockRequest()
        }
        return successLock
    }
}