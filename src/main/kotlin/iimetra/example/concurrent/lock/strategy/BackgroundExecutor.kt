package iimetra.example.concurrent.lock.strategy

import iimetra.example.concurrent.lock.wrapper.LockWrapper
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.apache.logging.log4j.LogManager

/**
 * Background executor on [DefaultEntityLocker].
 *
 * Repeats function [process] with period [repeatPeriod].
 */
abstract class BackgroundExecutor(private val repeatPeriod: Long) {

    private val logger = LogManager.getLogger(BackgroundExecutor::class.java)

    fun startWithExceptionCallback(lockMap: MutableMap<Any, LockWrapper>, onExceptionCallback: (Throwable) -> Unit) {
        launch {
            try {
                while (true) {
                    delay(repeatPeriod)
                    process(lockMap)
                }
            } catch (e: Throwable) {
                logger.error(e)
                e.printStackTrace()
                onExceptionCallback(e)
            }
        }
    }

    protected abstract fun process(lockMap: MutableMap<Any, LockWrapper>)
}

