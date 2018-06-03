package iimetra.example.concurrent.lock

import iimetra.example.concurrent.lock.locker.TimeoutEntityLocker
import iimetra.example.concurrent.lock.locker.lock
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit

class TimeoutEntityLockerTest {
    private val locker: TimeoutEntityLocker = EntityLockerFactory.create {
        repeatPeriod = TimeUnit.SECONDS.toMillis(1)
        withDeadlockPrevention()
    }

    @Test
    fun timeoutTestNotSuccess() {
        val entity1 = TestEntity(1)
        runBlocking {
            launch(newSingleThreadContext("ex1")) {
                locker.lock(entity1.id) {
                    delay(5000L)
                }
            }
            delay(1000L)
            locker.lock(1, TimeUnit.SECONDS, entity1.id) {
                entity1.value = "new"
            }
        }
        Assert.assertEquals("Value is not changed", "initial", entity1.value)
    }

    @Test
    fun timeoutTestSuccess() {
        val entity1 = TestEntity(1)
        runBlocking {
            launch(newSingleThreadContext("ex1")) {
                locker.lock(entity1.id) {
                    delay(3000)
                }
            }
            delay(1000L)
            locker.lock(5, TimeUnit.SECONDS, entity1.id) {
                entity1.value = "new"
            }
        }
        Assert.assertEquals("Value is changed", "new", entity1.value)
    }
}