package iimetra.example.concurrent.lock.wrapper

import java.util.*
import java.util.concurrent.TimeUnit
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
            forceLock()
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

    private fun forceLock() {
        var locked = innerLock.tryLock()
        while (!locked) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt()
                break
            }
            locked = innerLock.tryLock(1, TimeUnit.SECONDS)
        }
    }
}