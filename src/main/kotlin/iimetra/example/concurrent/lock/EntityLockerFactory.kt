package iimetra.example.concurrent.lock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class EntityLockerFactory {

    companion object {

        fun create(): TimeoutEntityLocker = create {}

        fun create(repeatPeriod: Long): TimeoutEntityLocker = create(repeatPeriod) {}

        fun create(builder: EntityLockBuilder.() -> Unit): TimeoutEntityLocker {
            val lockBuilder = EntityLockBuilder()
            lockBuilder.builder()
            return TimeoutEntityLockerDecorator(DefaultEntityLocker(lockBuilder.lockMap, lockBuilder.removeStrategyList))
        }

        // Not use default param values in order to java call possibility
        fun create(repeatPeriod: Long, builder: EntityLockBuilder.() -> Unit): TimeoutEntityLocker {
            val lockBuilder = EntityLockBuilder(repeatPeriod)
            lockBuilder.builder()
            return TimeoutEntityLockerDecorator(DefaultEntityLocker(lockBuilder.lockMap, lockBuilder.removeStrategyList))
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