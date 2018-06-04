package iimetra.example.concurrent.lock

import iimetra.example.concurrent.lock.locker.DefaultEntityLocker
import iimetra.example.concurrent.lock.locker.GlobalSupportEntityLockerDecorator
import iimetra.example.concurrent.lock.locker.TimeoutEntityLocker
import iimetra.example.concurrent.lock.locker.TimeoutEntityLockerDecorator
import iimetra.example.concurrent.lock.strategy.DeadLockStrategyExecutor
import iimetra.example.concurrent.lock.strategy.RemoveBySizeExecutor
import iimetra.example.concurrent.lock.strategy.RemoveByTimeExecutor
import iimetra.example.concurrent.lock.strategy.StrategyExecutor
import iimetra.example.concurrent.lock.wrapper.LockWrapper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Factory for creating entity locker.
 * Allows creating locker with standard and custom configurations.
 * */
class EntityLockerFactory {

    companion object {

        fun createFullyConfigured(): TimeoutEntityLocker = create {
            withDeadlockPrevention()
            withBySizeRemove(1000)
            withByTimeRemove(10, TimeUnit.SECONDS)
            repeatPeriod = TimeUnit.SECONDS.toMillis(1)
        }

        fun create(builder: EntityLockBuilder.() -> Unit): TimeoutEntityLocker {
            val lockBuilder = EntityLockBuilder()
            lockBuilder.builder()
            val initialLocker = DefaultEntityLocker(lockBuilder.lockMap, lockBuilder.strategyList.map { it() })
            val globalLocker = GlobalSupportEntityLockerDecorator(initialLocker)
            return TimeoutEntityLockerDecorator(globalLocker)
        }
    }
}

/**
 * Used for providing custom entity locker configuration.
 * */
class EntityLockBuilder {
    val lockMap = ConcurrentHashMap<Any, LockWrapper>()

    var strategyList = mutableListOf<() -> StrategyExecutor>()
        private set

    /** Period in seconds for repeating executor's processing tasks. For more information see [StrategyExecutor]. */
    var repeatPeriod: Long = TimeUnit.MINUTES.toMillis(5)

    /** Add removing elements from [DefaultEntityLocker]'s hashmap by time. */
    fun withByTimeRemove(duration: Long, timeUnit: TimeUnit) {
        strategyList.add { RemoveByTimeExecutor(repeatPeriod, timeUnit.toMillis(duration), lockMap) }
    }

    /** Add removing elements from [DefaultEntityLocker]'s hashmap by its size. */
    fun withBySizeRemove(maxSize: Int) {
        strategyList.add { RemoveBySizeExecutor(repeatPeriod, maxSize, lockMap) }
    }

    /** Add exceptions' throwing in case of deadlock. */
    fun withDeadlockPrevention() {
        strategyList.add { DeadLockStrategyExecutor(repeatPeriod, lockMap) }
    }
}