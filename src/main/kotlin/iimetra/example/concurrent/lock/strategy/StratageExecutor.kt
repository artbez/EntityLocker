package iimetra.example.concurrent.lock.strategy

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.apache.logging.log4j.LogManager

abstract class StrategyExecutor(@Volatile var repeatPeriod: Long) {

    private val logger = LogManager.getLogger(StrategyExecutor::class.java)

    fun start(onExceptionCallback: (Throwable) -> Unit) {
        launch {
            try {
                while (true) {
                    delay(repeatPeriod)
                    process()
                }
            } catch (e: Throwable) {
                logger.error(e)
                e.printStackTrace()
                onExceptionCallback(e)
            }
        }
    }

    protected abstract fun process()
}

