package iimetra.example.concurrent.lock.locker

import iimetra.example.concurrent.lock.strategy.StrategyExecutor
import iimetra.example.concurrent.lock.wrapper.LockWrapper
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

open class DefaultEntityLocker(
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>,
    strategyExecutors: List<StrategyExecutor>
) : EntityLocker {

    init {
        strategyExecutors.forEach {
            it.start {
                lockMap.values.forEach { it.lockStatistic.ownerThread?.interrupt() }
            }
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