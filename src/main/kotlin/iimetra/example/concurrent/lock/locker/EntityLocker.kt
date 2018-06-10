package iimetra.example.concurrent.lock.locker

import iimetra.example.concurrent.lock.strategy.BackgroundExecutor
import iimetra.example.concurrent.lock.wrapper.LockWrapper
import java.util.concurrent.ConcurrentHashMap

/**
 * Basic entity locker interface.
 * Provides methods for protected working with entities.
 */
interface EntityLocker {

    /**
     * Opens protected section for entity with id [entityId].
     * Waits if entity is already locked by another thread.
     *
     * Locking is reentrant operation.
     * */
    fun lock(entityId: Any)

    /** Try to open protected section for entity with id [entityId].
     *
     * @return <code>true</code> if success
     *         <code>false</code> if entity is already locked by another thread.
     * */
    fun tryLock(entityId: Any): Boolean

    /** Closes protected section for entity with id [entityId]. */
    fun unlock(entityId: Any)
}

/**
 * For a clear selection of the locking code area.
 *
 * locker.lock(id) {
 *    someWork
 * }
 * */
inline fun EntityLocker.lock(entityId: Any, protectedCode: () -> Unit) {
    lock(entityId)
    try {
        protectedCode()
    } finally {
        unlock(entityId)
    }
}


open class DefaultEntityLocker(backgroundExecutors: List<BackgroundExecutor>) : EntityLocker {

    private val lockMap = ConcurrentHashMap<Any, LockWrapper>()

    init {
        backgroundExecutors.forEach {
            it.startWithExceptionCallback(lockMap) {
                // If an exception has occurred interrupt all threads that own locks.
                lockMap.values.forEach { it.interruptOwningThread() }
            }
        }
    }

    override fun tryLock(entityId: Any): Boolean = lockMap.computeIfAbsent(entityId) { LockWrapper() }.tryLock()

    override fun lock(entityId: Any) {

        var successLock = false

        while (!successLock) {
            val lockWrapper = lockMap.computeIfAbsent(entityId) { LockWrapper() }
            successLock = lockWrapper.lock()
        }
    }

    override fun unlock(entityId: Any) = lockMap[entityId]?.unlock()
            ?: throw IllegalMonitorStateException("Entity with id $entityId is not locked")
}