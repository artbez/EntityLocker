package iimetra.example.concurrent.lock

import kotlinx.coroutines.experimental.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


private class TestEntity(val id: Long)

class EntityLockerTest {

    private lateinit var locker: EntityLocker

    @Before
    fun init() {

        locker = EntityLocker.create(TimeUnit.SECONDS.toMillis(1)) {
            withByTimeRemove(1, TimeUnit.SECONDS)
            withBySizeRemove(100)
        }
    }

    @Test
    fun differentEntitiesNotBlocking() {
        val entity1 = TestEntity(0)
        val entity2 = TestEntity(1)

        runBlocking {
            val countDownLatch = CountDownLatch(2)

            val workEntity1 = async {
                locker.lock(entity1.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                }
            }

            val workEntity2 = async {
                locker.lock(entity2.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                }
            }

            withTimeout(3000L) {
                workEntity1.await()
                workEntity2.await()
            }
        }
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
            println(lockMap.size)
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