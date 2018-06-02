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

        fun create(repeatPeriod: Long): TimeoutEntityLocker = create(repeatPeriod) {}

        fun create(builder: EntityLockBuilder.() -> Unit): TimeoutEntityLocker {
            val lockBuilder = EntityLockBuilder()
            lockBuilder.builder()
            return TimeoutEntityLockerDecorator(DefaultEntityLocker(lockBuilder.lockMap, lockBuilder.strategyList))
        }

        // Not use default param values in order to java call possibility
        fun create(repeatPeriod: Long, builder: EntityLockBuilder.() -> Unit): TimeoutEntityLocker {
            val lockBuilder = EntityLockBuilder(repeatPeriod)
            lockBuilder.builder()
            return TimeoutEntityLockerDecorator(DefaultEntityLocker(lockBuilder.lockMap, lockBuilder.strategyList))
        }
    }
}

class EntityLockBuilder(private val repeatPeriod: Long = TimeUnit.MINUTES.toMillis(5)) {
    val lockMap = ConcurrentHashMap<Any, LockWrapper>()

    var strategyList = mutableListOf<StrategyExecutor>()
        private set

    fun withByTimeRemove(duration: Long, timeUnit: TimeUnit) {
        val removeStrategy = RemoveByTimeExecutor(repeatPeriod, timeUnit.toMillis(duration), lockMap)
        strategyList.add(removeStrategy)
    }

    fun withBySizeRemove(maxSize: Int) {
        val removeStrategy = RemoveBySizeExecutor(repeatPeriod, maxSize, lockMap)
        strategyList.add(removeStrategy)
    }

    fun withDeadlockPrevention() {
        val deadLockStrategy = DeadLockStrategyExecutor(repeatPeriod, lockMap)
        strategyList.add(deadLockStrategy)
    }
}