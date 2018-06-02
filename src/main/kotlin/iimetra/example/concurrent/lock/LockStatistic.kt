package iimetra.example.concurrent.lock

class LockStatistic {
    var lastRequestTime = 0L
        private set

    var enters = 0
        private set

    fun visit() {
        enters++
        lastRequestTime = System.currentTimeMillis()
    }
}