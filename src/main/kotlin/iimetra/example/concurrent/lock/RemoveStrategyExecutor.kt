package iimetra.example.concurrent.lock

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.ConcurrentHashMap

sealed class RemoveStrategyExecutor(@Volatile var repeatPeriod: Long) {

    fun start() = launch {
        while (true) {
            delay(repeatPeriod)
            process()
        }
    }

    protected abstract fun process()
}

class RemoveByTimeExecutor(
    repeatPeriod: Long,
    private val liveTime: Long,
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>
) : RemoveStrategyExecutor(repeatPeriod) {

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
) : RemoveStrategyExecutor(repeatPeriod) {

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