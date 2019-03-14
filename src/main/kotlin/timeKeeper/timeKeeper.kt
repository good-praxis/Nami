package com.code.gamerg8.nami.timeKeeper

object TimeKeeper {
    private val launchTime: Long = System.currentTimeMillis()
    private val timeMap: MutableMap<String, Long> = hashMapOf()

    fun getDeltaSinceLaunch(): Long {
        return launchTime - System.currentTimeMillis()
    }

    fun getDeltaInSecondsSinceLaunch(): Long {
        return getDeltaSinceLaunch() / 1000
    }

    fun addStartTime(key: String) {
        checkKeyExists(key)
        timeMap.putIfAbsent(key, System.currentTimeMillis())
    }

    fun getDeltaFromKey(key: String): Long {
        checkKeyNull(key)
        return timeMap.getValue(key) - System.currentTimeMillis()
    }

    fun getDeltaInSecondsFromKey(key: String): Long {
        return getDeltaFromKey(key) / 1000
    }

    fun getDeltaSinceStartFromKey(key: String): Long {
        checkKeyNull(key)
        return launchTime - timeMap.getValue(key)
    }

    fun getDeltaSinceStartInSecondsFromKey(key: String): Long {
        return getDeltaSinceStartFromKey(key) / 1000
    }

    fun removeKey(key: String) {
        if(timeMap.containsKey(key)) {
            timeMap.remove(key)
        }
    }

    private fun checkKeyExists(key: String) {
        if(timeMap.contains(key)) {
            printWarning("Tried to add key '$key' that already exists! Stored time reset.")
            timeMap.replace(key, System.currentTimeMillis())
        }
    }

    private fun checkKeyNull(key: String) {
        if(timeMap[key] == null) {
            printWarning("Tried to get delta from uninitialized key '$key'. Initialized with current system time.")
            addStartTime(key)
        }
    }

    private fun printWarning(message: String) {
        println("[Timekeeper] $message")
    }
}