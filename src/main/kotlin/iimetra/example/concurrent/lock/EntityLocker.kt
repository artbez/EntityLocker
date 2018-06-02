package iimetra.example.concurrent.lock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface EntityLockerInterface {
    fun lock(entityId: Any)
    fun unlock(entityId: Any)
}

class EntityLocker private constructor(
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>,
    private val removeStrategyList: List<RemoveStrategy>
) : EntityLockerInterface {

    override fun lock(entityId: Any) {
        processStopCondition(entityId)

        var successLock = false
        while (!successLock) {
            val lockWrapper = lockMap.computeIfAbsent(entityId) { LockWrapper() }
            successLock = lockWrapper.lock()
        }
    }

    override fun unlock(entityId: Any) {
        val entityLock = lockMap[entityId] ?: throw IllegalStateException("Can't unlock without monitor")
        entityLock.unlock()
    }

    inline fun lock(entityId: Any, protectedCode: () -> Unit) {
        lock(entityId)
        try {
            protectedCode()
        } finally {
            unlock(entityId)
        }
    }

    private fun processStopCondition(entityId: Any) {
        removeStrategyList
            // for lazy filtering
            .stream()
            .filter { it.checkStopCondition(entityId) }
            .forEach {
                it.processStrategy(entityId)
            }
    }

    companion object {

        fun create(): EntityLocker {
            val lockBuilder = EntityLockBuilder()
            return EntityLocker(lockBuilder.lockMap, lockBuilder.removeStrategyList)
        }

        fun create(builder: EntityLockBuilder.() -> Unit): EntityLocker {
            val lockBuilder = EntityLockBuilder()
            lockBuilder.builder()
            return EntityLocker(lockBuilder.lockMap, lockBuilder.removeStrategyList)
        }
    }
}

class EntityLockBuilder {
    val lockMap = ConcurrentHashMap<Any, LockWrapper>()

    var removeStrategyList = mutableListOf<RemoveStrategy>()
        private set

    fun withByTimeRemove(duration: Long, timeUnit: TimeUnit) {
        val removeStrategy = RemoveByTimeStrategy(timeUnit.toMillis(duration), lockMap)
        removeStrategyList.add(removeStrategy)
    }

    fun withBySizeRemove(maxSize: Int) {
        val removeStrategy = RemoveBySizeStrategy(maxSize, lockMap)
        removeStrategyList.add(removeStrategy)
    }
}