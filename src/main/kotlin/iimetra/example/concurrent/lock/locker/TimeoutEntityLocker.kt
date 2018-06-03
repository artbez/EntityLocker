package iimetra.example.concurrent.lock.locker

import java.util.concurrent.TimeUnit

interface TimeoutEntityLocker : GlobalSupportEntityLocker {
    fun lock(timeout: Long, timeUnit: TimeUnit, entityId: Any): Boolean
    fun lock(timeout: Long, timeUnit: TimeUnit): Boolean
}

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

    override fun lock(timeout: Long, timeUnit: TimeUnit): Boolean = repeatTimedRequest(timeout, timeUnit) { tryLock() }

    override fun lock(timeout: Long, timeUnit: TimeUnit, entityId: Any): Boolean = repeatTimedRequest(timeout, timeUnit) { tryLock(entityId) }

    private inline fun repeatTimedRequest(timeout: Long, timeUnit: TimeUnit, lockRequest: GlobalSupportEntityLocker.() -> Boolean): Boolean {
        val finishTime = System.currentTimeMillis() + timeUnit.toMillis(timeout)
        var successLock = false
        while (!successLock && System.currentTimeMillis() < finishTime) {
            successLock = locker.lockRequest()
        }
        return successLock
    }
}