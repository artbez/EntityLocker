package iimetra.example.concurrent.lock

import iimetra.example.concurrent.lock.locker.EntityLocker
import iimetra.example.concurrent.lock.locker.lock
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// EntityLocker is thread-based, not coroutine-based. So, for correct testing, we should create a thread per coroutine.
class DeadLockTest {

    private val locker: EntityLocker = EntityLockerFactory.create {
        withByTimeRemove(1, TimeUnit.SECONDS)
        withBySizeRemove(100)
        withDeadlockPrevention()
        repeatPeriod = TimeUnit.SECONDS.toMillis(1)
    }

    @Test(expected = InterruptedException::class)
    fun deadLockExecutorFindLock() {
        val entity1 = TestEntity(0)
        val entity2 = TestEntity(1)

        runBlocking {

            val countDownLatch = CountDownLatch(2)

            val workEntity1 = async(newSingleThreadContext("ex1")) {
                locker.lock(entity1.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    locker.lock(entity2.id) {
                        entity1.value = "v1"
                    }
                }
            }

            val workEntity2 = async(newSingleThreadContext("ex2")) {
                locker.lock(entity2.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    locker.lock(entity1.id) {
                        entity2.value = "v2"
                    }
                }
            }

            workEntity1.await()
            workEntity2.await()
        }
    }

    @Test(expected = InterruptedException::class)
    fun deadLockExecutorFindLock2() {
        val entity1 = TestEntity(0)
        val entity2 = TestEntity(1)
        val entity3 = TestEntity(2)

        runBlocking {

            val countDownLatch = CountDownLatch(3)

            val workEntity1 = async(newSingleThreadContext("ex1")) {
                locker.lock(entity1.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    locker.lock(entity2.id) {
                        entity1.value = "v1"
                    }
                }
            }

            val workEntity2 = async(newSingleThreadContext("ex2")) {
                locker.lock(entity2.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    locker.lock(entity3.id) {
                        entity2.value = "v2"
                    }
                }
            }

            val workEntity3 = async(newSingleThreadContext("ex3")) {
                locker.lock(entity3.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    locker.lock(entity1.id) {
                        entity3.value = "v3"
                    }
                }
            }

            workEntity1.await()
            workEntity2.await()
            workEntity3.await()
        }
    }

    @Test
    fun noDeadLock() {
        val entity1 = TestEntity(0)
        val entity2 = TestEntity(1)
        val entity3 = TestEntity(2)

        runBlocking {

            val countDownLatch = CountDownLatch(3)

            val workEntity1 = async(newSingleThreadContext("ex1")) {
                locker.lock(entity1.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    locker.lock(entity2.id) {
                        entity1.value = "v1"
                    }
                }
            }

            val workEntity2 = async(newSingleThreadContext("ex2")) {
                locker.lock(entity2.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    locker.lock(entity3.id) {
                        entity2.value = "v2"
                    }
                }
            }

            val workEntity3 = async(newSingleThreadContext("ex3")) {
                locker.lock(entity3.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                    delay(5, TimeUnit.SECONDS)
                    entity3.value = "v3"
                }
            }

            workEntity1.await()
            workEntity2.await()
            workEntity3.await()

            Assert.assertEquals("Entity1 should change value", "v1", entity1.value)
            Assert.assertEquals("Entity2 should change value", "v2", entity2.value)
            Assert.assertEquals("Entity3 should change value", "v3", entity3.value)
        }
    }
}