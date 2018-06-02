package iimetra.example.concurrent.lock.wrapper

import java.util.*
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.locks.ReentrantLock

class LockWrapper {

    val uid = UUID.randomUUID().toString();
    val lockStatistic = LockStatistic()
    private val innerLock = ReentrantLock()
    private val visitorsAndIsDeleted = AtomicMarkableReference<Long>(0, false)

    fun lock(): Boolean {
        val lastVisitorsNumber = visitorsAndIsDeleted.reference
        val successVisit = visitorsAndIsDeleted.compareAndSet(lastVisitorsNumber, lastVisitorsNumber + 1, false, false)
        if (successVisit) {
            lockStatistic.request()
            innerLock.lock()
            lockStatistic.visit()
        }
        return successVisit
    }

    fun tryLock(): Boolean {
        val lastVisitorsNumber = visitorsAndIsDeleted.reference
        val successVisit = visitorsAndIsDeleted.compareAndSet(lastVisitorsNumber, lastVisitorsNumber + 1, false, false)
        if (successVisit) {
            lockStatistic.request()
            val locked = innerLock.tryLock()
            if (locked) {
                lockStatistic.visit()
            }
            return locked
        }
        return successVisit
    }

    fun unlock() {
        lockStatistic.leave()
        innerLock.unlock()

        var successExit = false
        while (!successExit) {
            val lastVisitorsNumber = visitorsAndIsDeleted.reference
            successExit = visitorsAndIsDeleted.compareAndSet(lastVisitorsNumber, lastVisitorsNumber - 1, false, false)
        }
    }

    // If we cannot remove, element is used by another thread and this means we should not remove it
    fun tryRemove(): Boolean = visitorsAndIsDeleted.compareAndSet(0, 0, false, true)

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LockWrapper

        if (uid != other.uid) return false

        return true
    }
}