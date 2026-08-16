package com.kensukeyoshida.onomatopoeiadetector.data

/**
 * 保存時刻を単調増加で発行する。
 *
 * `System.currentTimeMillis()` はミリ秒までしか刻めず、続けて保存すると同じ値になる。
 * 日付が同値だと `ORDER BY date DESC` の並びが決まらず、新しい順に並べることも、
 * 古い方から間引くこともできなくなる。
 */
class MonotonicClock(private val now: () -> Long = System::currentTimeMillis) {

    private var lastIssued = 0L

    @Synchronized
    fun next(): Long {
        val current = now()
        lastIssued = if (current > lastIssued) current else lastIssued + 1
        return lastIssued
    }
}
