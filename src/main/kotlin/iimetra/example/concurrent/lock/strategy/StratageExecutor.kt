package iimetra.example.concurrent.lock.strategy

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

abstract class StrategyExecutor(@Volatile var repeatPeriod: Long) {

    fun start() = launch {
        while (true) {
            delay(repeatPeriod)
            process()
        }
    }

    protected abstract fun process()
}