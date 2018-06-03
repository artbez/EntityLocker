package iimetra.example.concurrent.lock

import iimetra.example.concurrent.lock.strategy.DeadLockStrategyExecutor
import iimetra.example.concurrent.lock.strategy.RemoveBySizeExecutor
import iimetra.example.concurrent.lock.strategy.RemoveByTimeExecutor
import iimetra.example.concurrent.lock.strategy.StrategyExecutor
import iimetra.example.concurrent.lock.wrapper.LockWrapper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class EntityLockerFactory {

    companion object {

        fun create(): TimeoutEntityLocker = create {}

        // for java
        fun createFull(): TimeoutEntityLocker = create {
            withDeadlockPrevention()
            withBySizeRemove(1000)
            withByTimeRemove(10, TimeUnit.SECONDS)
            withDeadlockPrevention()
            repeatPeriod = TimeUnit.SECONDS.toMillis(1)
        }

        fun create(builder: EntityLockBuilder.() -> Unit): TimeoutEntityLocker {
            val lockBuilder = EntityLockBuilder()
            lockBuilder.builder()
            return TimeoutEntityLockerDecorator(DefaultEntityLocker(lockBuilder.lockMap, lockBuilder.strategyList.map { it() }))
        }
    }
}

class EntityLockBuilder {
    val lockMap = ConcurrentHashMap<Any, LockWrapper>()

    var strategyList = mutableListOf<() -> StrategyExecutor>()
        private set

    var repeatPeriod: Long = TimeUnit.MINUTES.toMillis(5)

    fun withByTimeRemove(duration: Long, timeUnit: TimeUnit) {
        strategyList.add { RemoveByTimeExecutor(repeatPeriod, timeUnit.toMillis(duration), lockMap) }
    }

    fun withBySizeRemove(maxSize: Int) {
        strategyList.add { RemoveBySizeExecutor(repeatPeriod, maxSize, lockMap) }
    }

    fun withDeadlockPrevention() {
        strategyList.add { DeadLockStrategyExecutor(repeatPeriod, lockMap) }
    }
}