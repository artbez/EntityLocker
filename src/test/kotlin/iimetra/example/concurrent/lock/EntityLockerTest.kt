package iimetra.example.concurrent.lock

import iimetra.example.concurrent.lock.locker.EntityLocker
import iimetra.example.concurrent.lock.locker.lock
import kotlinx.coroutines.experimental.*
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class TestEntity(val id: Long, var value: String = "initial")

class EntityLockerTest {

    private var locker: EntityLocker = EntityLockerFactory.create {
        repeatPeriod = TimeUnit.SECONDS.toMillis(1)
        withByTimeRemove(1, TimeUnit.SECONDS)
        withBySizeRemove(100)
        withDeadlockPrevention()
    }

    @Test
    fun differentEntitiesNotBlocking() {
        val entity1 = TestEntity(0)
        val entity2 = TestEntity(1)

        runBlocking {
            val countDownLatch = CountDownLatch(2)

            val workEntity1 = async(newSingleThreadContext("ex1")) {
                locker.lock(entity1.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    entity1.value = "v1"
                }
            }

            val workEntity2 = async(newSingleThreadContext("ex2")) {
                locker.lock(entity2.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    entity2.value = "v2"
                }
            }

            withTimeout(3000L) {
                workEntity1.await()
                workEntity2.await()
            }
        }

        Assert.assertEquals("Entity1 should change value", "v1", entity1.value)
        Assert.assertEquals("Entity2 should change value", "v2", entity2.value)
    }

    @Test
    fun reentrantLockingAvailable() {
        val entity1 = TestEntity(0)
        runBlocking {
            val workEntity = launch {
                locker.lock(entity1.id) {
                    locker.lock(entity1.id) {
                        entity1.value = "reentrant"
                    }
                }
            }
            withTimeout(3000L) {
                workEntity.join()
            }
        }

        Assert.assertEquals("Entity1 should change value", "reentrant", entity1.value)
    }

    @Test
    fun timeRemovingStrategyCheck() {
        locker = EntityLockerFactory.create {
            repeatPeriod = TimeUnit.SECONDS.toMillis(1)
            withByTimeRemove(1, TimeUnit.SECONDS)
            withDeadlockPrevention()
        }

        runBlocking {
            for (i in 0..10000) {
                launch {
                    locker.lock(i) {}
                }
            }
            val lockMap = getLockerMap()

            delay(5000)
            Assert.assertTrue("Elements of hashmap should removed", lockMap.size == 0)
        }
    }

    @Test
    fun sizeRemovingStrategyCheck() {
        locker = EntityLockerFactory.create {
            repeatPeriod = TimeUnit.SECONDS.toMillis(1)
            withBySizeRemove(100)
            withDeadlockPrevention()
        }

        runBlocking {
            for (i in 0..10000) {
                launch {
                    locker.lock(i) {}
                }
            }

            val lockMap = getLockerMap()

            delay(5000L)
            Assert.assertTrue("Elements of hashmap should removed", lockMap.size <= 100)
        }
    }

    private fun getLockerMap(): ConcurrentHashMap<*, *> {
        val globalLockerField = locker.javaClass.getDeclaredField("locker")
        globalLockerField.isAccessible = true
        val globalLocker = globalLockerField.get(locker)

        val lockerField = globalLocker.javaClass.getDeclaredField("locker")
        lockerField.isAccessible = true
        val entryLocker = lockerField.get(globalLocker)

        val lockMapField = entryLocker.javaClass.getDeclaredField("lockMap")
        lockMapField.isAccessible = true

        return lockMapField.get(entryLocker) as ConcurrentHashMap<*, *>
    }
}