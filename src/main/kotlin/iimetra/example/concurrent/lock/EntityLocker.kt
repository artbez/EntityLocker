package iimetra.example.concurrent.lock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface EntityLockerInterface {
    fun lock(entityId: Any)
    fun unlock(entityId: Any)
}

class EntityLocker private constructor(
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>,
    removeStrategyExecutorList: List<RemoveStrategyExecutor>
) : EntityLockerInterface {

    init {
        removeStrategyExecutorList.forEach {
            it.start()
        }
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

    inline fun lock(entityId: Any, protectedCode: () -> Unit) {
        lock(entityId)
        try {
            protectedCode()
        } finally {
            unlock(entityId)
        }
    }

    companion object {

        fun create(): EntityLocker = create {}

        fun create(repeatPeriod: Long): EntityLocker = create(repeatPeriod) {}

        fun create(builder: EntityLockBuilder.() -> Unit): EntityLocker {
            val lockBuilder = EntityLockBuilder()
            lockBuilder.builder()
            return EntityLocker(lockBuilder.lockMap, lockBuilder.removeStrategyList)
        }

        // Not use default param values in order to java call possibility
        fun create(repeatPeriod: Long, builder: EntityLockBuilder.() -> Unit): EntityLocker {
            val lockBuilder = EntityLockBuilder(repeatPeriod)
            lockBuilder.builder()
            return EntityLocker(lockBuilder.lockMap, lockBuilder.removeStrategyList)
        }
    }
}

class EntityLockBuilder(private val repeatPeriod: Long = TimeUnit.MINUTES.toMillis(5)) {
    val lockMap = ConcurrentHashMap<Any, LockWrapper>()

    var removeStrategyList = mutableListOf<RemoveStrategyExecutor>()
        private set

    fun withByTimeRemove(duration: Long, timeUnit: TimeUnit) {
        val removeStrategy = RemoveByTimeExecutor(repeatPeriod, timeUnit.toMillis(duration), lockMap)
        removeStrategyList.add(removeStrategy)
    }

    fun withBySizeRemove(maxSize: Int) {
        val removeStrategy = RemoveBySizeExecutor(repeatPeriod, maxSize, lockMap)
        removeStrategyList.add(removeStrategy)
    }
}