package iimetra.example.concurrent.lock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class EntityLocker {

    companion object {

        private val lockMap = ConcurrentHashMap<Any, ReentrantLock>()

        fun lock(entityId: Any) {
            lockMap.computeIfAbsent(entityId) {
                ReentrantLock()
            }.lock()
        }

        fun unlock(entityId: Any) {
            lockMap.computeIfAbsent(entityId) {
                ReentrantLock()
            }.unlock()
        }
    }
}

inline fun lock(entityId: Any, protectedCode: () -> Unit) {
    EntityLocker.lock(entityId)
    try {
        protectedCode()
    } finally {
        EntityLocker.unlock(entityId)
    }
}