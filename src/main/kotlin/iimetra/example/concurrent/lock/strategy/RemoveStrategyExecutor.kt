package iimetra.example.concurrent.lock.strategy

import iimetra.example.concurrent.lock.wrapper.LockWrapper
import java.util.concurrent.ConcurrentHashMap

/**
 * For Removing elements from [DefaultEntityLocker]
 *
 * Removes element if its last request time plus [liveTime] is lower than nowtime
 */
class RemoveByTimeExecutor(
    repeatPeriod: Long,
    private val liveTime: Long,
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>
) : StrategyExecutor(repeatPeriod) {

    override fun process() {
        // stream for lazy
        lockMap.entries.stream()
            .filter { (_, v) -> v.checkCondition() }
            .forEach { (e, v) -> removeAttempt(e, v) }
    }

    private fun LockWrapper.checkCondition(): Boolean = lockStatistic.lastOwningTime + liveTime < System.currentTimeMillis()

    private fun removeAttempt(entityId: Any, wrapper: LockWrapper) {
        if (wrapper.tryRemove()) {
            lockMap.remove(entityId, wrapper)
        }
    }
}

/**
 * For Removing elements from [DefaultEntityLocker]
 *
 * Try to remove all elements if [lockMap] size is bigger than [maxSize]
 */
class RemoveBySizeExecutor(
    repeatPeriod: Long,
    private val maxSize: Int,
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>
) : StrategyExecutor(repeatPeriod) {

    override fun process() {
        if (lockMap.size > maxSize) {
            removeAttempt()
        }
    }

    private fun removeAttempt() {
        lockMap.entries.forEach { (entityId, wrapper) ->
            if (wrapper.tryRemove()) {
                lockMap.remove(entityId, wrapper)
            }
        }
    }
}