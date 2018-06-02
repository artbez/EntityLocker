package iimetra.example.concurrent.lock

import java.util.concurrent.ConcurrentHashMap

sealed class RemoveStrategy {
    abstract fun checkStopCondition(entityId: Any): Boolean
    abstract fun processStrategy(entityId: Any)
}

class RemoveByTimeStrategy(
    private val periodInSeconds: Long,
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>
) : RemoveStrategy() {

    override fun checkStopCondition(entityId: Any): Boolean {
        val wrapper = lockMap[entityId] ?: return false
        return wrapper.lockStatistic.lastRequestTime + periodInSeconds < System.currentTimeMillis()
    }

    override fun processStrategy(entityId: Any) {
        val wrapper = lockMap[entityId] ?: return
        if (wrapper.tryRemove()) {
            lockMap.remove(entityId, wrapper)
        }
    }
}

class RemoveBySizeStrategy(
    private val maxSize: Int,
    private val lockMap: ConcurrentHashMap<Any, LockWrapper>
) : RemoveStrategy() {

    override fun checkStopCondition(entityId: Any): Boolean = (lockMap.size > maxSize)

    override fun processStrategy(entityId: Any) {
        lockMap.entries.forEach { (entityId, wrapper) ->
            if (wrapper.tryRemove()) {
                lockMap.remove(entityId, wrapper)
            }
        }
    }
}