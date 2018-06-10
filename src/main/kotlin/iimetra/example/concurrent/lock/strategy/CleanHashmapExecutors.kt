package iimetra.example.concurrent.lock.strategy

import iimetra.example.concurrent.lock.wrapper.LockWrapper

/**
 * For Removing LockWrappers from [DefaultEntityLocker] elements map.
 *
 * Deletes an item if it has not been accessed by [elementLivinigTime].
 */
class RemoveByTimeExecutor(repeatPeriod: Long, private val elementLivinigTime: Long) : BackgroundExecutor(repeatPeriod) {

    override fun process(lockMap: MutableMap<Any, LockWrapper>) {
        lockMap.entries.forEach { (entityId, lockWrapper) ->

            if (lockWrapper.notVisitedLongTime() && lockWrapper.tryRemove()) {
                lockMap.remove(entityId, lockWrapper)
            }
        }
    }

    private fun LockWrapper.notVisitedLongTime(): Boolean = lockStatistic.lastOwningTime + elementLivinigTime < System.currentTimeMillis()
}

/**
 * For Removing elements from [DefaultEntityLocker].
 *
 * Try to remove all elements if [lockMap] size is bigger than [maxSize].
 */
class RemoveBySizeExecutor(repeatPeriod: Long, private val maxSize: Int) : BackgroundExecutor(repeatPeriod) {

    override fun process(lockMap: MutableMap<Any, LockWrapper>) {
        if (lockMap.size > maxSize) {
            removeAttempt(lockMap)
        }
    }

    private fun removeAttempt(lockMap: MutableMap<Any, LockWrapper>) {
        lockMap.entries.forEach { (entityId, wrapper) ->
            if (wrapper.tryRemove()) {
                lockMap.remove(entityId, wrapper)
            }
        }
    }
}