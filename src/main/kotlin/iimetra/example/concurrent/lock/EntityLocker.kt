package iimetra.example.concurrent.lock

import java.util.concurrent.ConcurrentHashMap

interface EntityLocker {
    fun lock(entityId: Any)
    fun tryLock(entityId: Any): Boolean
    fun unlock(entityId: Any)
}

inline fun EntityLocker.lock(entityId: Any, protectedCode: () -> Unit) {
    lock(entityId)
    try {
        protectedCode()
    } finally {
        unlock(entityId)
    }
}

class DefaultEntityLocker(
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>,
    removeStrategyExecutorList: List<RemoveStrategyExecutor>
) : EntityLocker {

    init {
        removeStrategyExecutorList.forEach {
            it.start()
        }
    }

    override fun tryLock(entityId: Any): Boolean {
        val lockWrapper = lockMap.computeIfAbsent(entityId) { LockWrapper() }
        return lockWrapper.tryLock()
    }

    override fun lock(entityId: Any) {
        var successLock = false
        while (!successLock) {
            val lockWrapper = lockMap.computeIfAbsent(entityId) { LockWrapper() }
            successLock = lockWrapper.lock()
        }
    }

    override fun unlock(entityId: Any) {
        val entityLock = lockMap[entityId] ?: throw IllegalStateException("Entity id is not locked")
        entityLock.unlock()
    }
}