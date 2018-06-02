package iimetra.example.concurrent.lock

import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.locks.ReentrantLock

class LockWrapper {

    val lockStatistic = LockStatistic()
    private val innerLock = ReentrantLock()
    private val visitorsAndIsDeleted = AtomicMarkableReference<Long>(0, false)

    fun lock(): Boolean {
        val lastVisitorsNumber = visitorsAndIsDeleted.reference
        val successVisit = visitorsAndIsDeleted.compareAndSet(lastVisitorsNumber, lastVisitorsNumber + 1, false, false)
        if (successVisit) {
            innerLock.lock()
            lockStatistic.visit()
        }
        return successVisit
    }

    fun tryLock(): Boolean {
        val lastVisitorsNumber = visitorsAndIsDeleted.reference
        val successVisit = visitorsAndIsDeleted.compareAndSet(lastVisitorsNumber, lastVisitorsNumber + 1, false, false)
        if (successVisit) {
            val locked = innerLock.tryLock()
            if (locked) {
                lockStatistic.visit()
            }
            return locked
        }
        return successVisit
    }

    fun unlock() {
        innerLock.unlock()

        var successExit = false
        while (!successExit) {
            val lastVisitorsNumber = visitorsAndIsDeleted.reference
            successExit = visitorsAndIsDeleted.compareAndSet(lastVisitorsNumber, lastVisitorsNumber - 1, false, false)
        }
    }

    // If we cannot remove, element is used by another thread and this means we should not remove it
    fun tryRemove(): Boolean = visitorsAndIsDeleted.compareAndSet(0, 0, false, true)
}