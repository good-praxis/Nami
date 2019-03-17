package com.code.gamerg8.nami.utils

import java.sql.Time

class Stopwatch{
    val timestamps = mutableMapOf<String, Long>()

    fun setTimestamp(name: String){
        timestamps[name] = System.nanoTime()
    }

    fun elapsedSince(timestamp: String): Long {
        return timestamps[timestamp]!! - System.nanoTime()
    }
}

class Timespan(val nanoSeconds: Long) {
    val milliseconds: Double
        get() {
            return nanoSeconds.toDouble() / 1_000_000
        }

    val seconds: Double
        get() {
            return nanoSeconds.toDouble() / 1_000_000_000
        }
}