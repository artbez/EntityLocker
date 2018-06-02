package iimetra.example.concurrent.lock.strategy

import iimetra.example.concurrent.lock.wrapper.LockWrapper
import java.util.concurrent.ConcurrentHashMap

class RemoveByTimeExecutor(
    repeatPeriod: Long,
    private val liveTime: Long,
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>
) : StrategyExecutor(repeatPeriod) {

    override fun process() {
        // stream for lazy filter
        lockMap.entries.stream()
            .filter { (_, v) -> v.checkCondition() }
            .forEach { (e, v) -> removeAttempt(e, v) }
    }

    private fun LockWrapper.checkCondition(): Boolean {
        return lockStatistic.lastRequestTime + liveTime < System.currentTimeMillis()
    }

    private fun removeAttempt(entityId: Any, wrapper: LockWrapper) {
        if (wrapper.tryRemove()) {
            lockMap.remove(entityId, wrapper)
        }
    }
}

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