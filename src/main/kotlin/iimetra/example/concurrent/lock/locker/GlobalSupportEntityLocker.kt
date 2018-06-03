package iimetra.example.concurrent.lock.locker

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

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
    private val global = AtomicBoolean(false)
    private val lockCondition = java.lang.Object()
    private val lockerVisitors = AtomicLong(0)

    override fun lock() {
        var success = global.compareAndSet(false, true)
        while (!success) {
            lockCondition.wait()
            success = global.compareAndSet(false, true)
        }
        runBlocking {
            while (lockerVisitors.get() != 0L) {
                delay(100)
            }
        }
    }

    override fun unlock() {
        global.set(false)
        lockCondition.notifyAll()
    }

    override fun tryLock(): Boolean {
        if (global.compareAndSet(false, true)) {
            if (lockerVisitors.get() == 0L) {
                return true
            }
            global.compareAndSet(true, false)
        }
        return false
    }

    override fun lock(entityId: Any) {
        if (!global.get()) {
            lockerVisitors.incrementAndGet()
            locker.lock(entityId)
        }
    }

    override fun tryLock(entityId: Any): Boolean {
        if (!global.get()) {
            return locker.tryLock(entityId)
        }
        return false
    }

    override fun unlock(entityId: Any) {
        locker.unlock(entityId)
        lockerVisitors.decrementAndGet()
    }
}