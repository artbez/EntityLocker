package iimetra.example.concurrent.lock

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


private class TestEntity(val id: Long, var value: String = "initial")

class EntityLockerTest {

    private val locker: EntityLocker = EntityLocker.create(TimeUnit.SECONDS.toMillis(1)) {
        withByTimeRemove(1, TimeUnit.SECONDS)
        withBySizeRemove(100)
    }

    @Test
    fun differentEntitiesNotBlocking() {
        val entity1 = TestEntity(0)
        val entity2 = TestEntity(1)

        runBlocking {
            val countDownLatch = CountDownLatch(2)

            val workEntity1 = launch {
                locker.lock(entity1.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    entity1.value = "v1"
                }
            }

            val workEntity2 = launch {
                locker.lock(entity2.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    entity2.value = "v2"
                }
            }

            withTimeout(3000L) {
                workEntity1.join()
                workEntity2.join()
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
        runBlocking {
            for (i in 0..10000) {
                launch {
                    locker.lock(i) {}
                }
            }
            val lockMapField = locker.javaClass.getDeclaredField("lockMap")
            lockMapField.isAccessible = true
            val lockMap = lockMapField.get(locker) as ConcurrentHashMap<*, *>

            delay(5000)
            Assert.assertTrue("Elements of hashmap should removed", lockMap.size == 0)
        }
    }

    @Test
    fun sizeRemovingStrategyCheck() {
        runBlocking {
            for (i in 0..10000) {
                launch {
                    locker.lock(i) {}
                }
            }
            val lockMapField = locker.javaClass.getDeclaredField("lockMap")
            lockMapField.isAccessible = true
            val lockMap = lockMapField.get(locker) as ConcurrentHashMap<*, *>

            delay(5000L)
            Assert.assertTrue("Elements of hashmap should removed", lockMap.size <= 100)
        }
    }
}