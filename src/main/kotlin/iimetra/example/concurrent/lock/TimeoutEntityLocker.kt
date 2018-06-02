package iimetra.example.concurrent.lock

import java.util.concurrent.TimeUnit

interface TimeoutEntityLocker : EntityLocker {
    fun lock(timeout: Long, timeUnit: TimeUnit, entityId: Any): Boolean
}

class TimeoutEntityLockerDecorator(private val locker: DefaultEntityLocker) : EntityLocker by (locker), TimeoutEntityLocker {

    override fun lock(timeout: Long, timeUnit: TimeUnit, entityId: Any): Boolean {
        val finishTime = System.currentTimeMillis() + timeUnit.toMillis(timeout)
        var successLock = false
        while (!successLock || System.currentTimeMillis() < finishTime) {
            successLock = locker.tryLock(entityId)
        }
        return successLock
    }
}