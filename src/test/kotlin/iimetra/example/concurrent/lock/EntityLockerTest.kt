package iimetra.example.concurrent.lock

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

private class TestEntity(val id: Long, var count: Int = 0)

class EntityLockerTest {

    private lateinit var entity1: TestEntity
    private lateinit var entity2: TestEntity

    @Before
    fun init() {
        entity1 = TestEntity(0)
        entity2 = TestEntity(0)
    }

    @Test
    fun differentEntitiesNotBlocking() {
        runBlocking {

            val countDownLatch = CountDownLatch(2)

            launch {
                lock(entity1.id) {
                    countDownLatch.countDown()
                }
            }

            launch {
                lock(entity2.id) {
                    countDownLatch.countDown()
                }
            }

            countDownLatch.await()
        }
    }
}