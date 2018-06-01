package iimetra.example.concurrent.lock

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

private class TestEntity(val id: Long)

class EntityLockerTest {

    private lateinit var entity1: TestEntity
    private lateinit var entity2: TestEntity

    @Before
    fun init() {
        entity1 = TestEntity(0)
        entity2 = TestEntity(1)
    }

    @Test
    fun differentEntitiesNotBlocking() {
        runBlocking {
            val countDownLatch = CountDownLatch(2)

            val workEntity1 = async {
                lock(entity1.id) {
                    countDownLatch.countDown()
                    countDownLatch.await()
                }
            }

            val workEntity2 = async {
                lock(entity2.id) {
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
}