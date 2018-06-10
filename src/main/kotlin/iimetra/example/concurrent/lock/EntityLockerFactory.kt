package iimetra.example.concurrent.lock

import iimetra.example.concurrent.lock.locker.*
import iimetra.example.concurrent.lock.strategy.BackgroundExecutor
import iimetra.example.concurrent.lock.strategy.DeadLockBackgroundExecutor
import iimetra.example.concurrent.lock.strategy.RemoveBySizeExecutor
import iimetra.example.concurrent.lock.strategy.RemoveByTimeExecutor
import java.util.concurrent.TimeUnit

/**
 * Factory for creating entity locker.
 * Allows creating locker with standard and custom configurations.
 * */
class EntityLockerFactory {

    companion object {

        fun createWithDefaultConfiguration(): TimeoutEntityLocker = create(repeatPeriod = TimeUnit.SECONDS.toMillis(1)) {
            withDeadlockPrevention()
            withBySizeRemove(1000)
            withByTimeRemove(10, TimeUnit.SECONDS)
        }

        fun create(repeatPeriod: Long, builder: EntityLockBuilder.() -> Unit): TimeoutEntityLocker {

            val lockBuilder = EntityLockBuilder(repeatPeriod)
            lockBuilder.builder()

            return DefaultEntityLocker(lockBuilder.strategyList)
                .withGlobalSupport()
                .withLocalTimeoutSupport()
        }

        private fun EntityLocker.withGlobalSupport() = GlobalSupportEntityLockerDecorator(this)
        private fun GlobalSupportEntityLocker.withLocalTimeoutSupport() = TimeoutEntityLockerDecorator(this)
    }
}

/**
 * Used for providing custom entity locker configuration.
 * [repeatPeriod] is a time duration in millis for repeating executors processing tasks.
 * */
class EntityLockBuilder(private val repeatPeriod: Long) {

    val strategyList = mutableListOf<BackgroundExecutor>()

    /** Add removing elements from [DefaultEntityLocker]'s lockMap by time. */
    fun withByTimeRemove(duration: Long, timeUnit: TimeUnit) {
        strategyList.add(RemoveByTimeExecutor(repeatPeriod, timeUnit.toMillis(duration)))
    }

    /** Add removing elements from [DefaultEntityLocker]'s lockMap by its size. */
    fun withBySizeRemove(maxSize: Int) {
        strategyList.add(RemoveBySizeExecutor(repeatPeriod, maxSize))
    }

    /** Add exceptions' throwing in case of deadlock. */
    fun withDeadlockPrevention() {
        strategyList.add(DeadLockBackgroundExecutor(repeatPeriod))
    }
}