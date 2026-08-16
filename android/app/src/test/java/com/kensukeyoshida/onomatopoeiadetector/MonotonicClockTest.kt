package com.kensukeyoshida.onomatopoeiadetector

import com.kensukeyoshida.onomatopoeiadetector.data.MonotonicClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonotonicClockTest {

    /** 時計が止まっていても、発行する値は必ず進む。 */
    @Test
    fun `issues increasing values even when the clock does not advance`() {
        val clock = MonotonicClock { 1_000L }
        val issued = List(105) { clock.next() }

        assertEquals(105, issued.toSet().size)
        assertEquals(issued.sorted(), issued)
        assertEquals(1_000L, issued.first())
    }

    /** 時計が進んだら実時刻に追いつく。 */
    @Test
    fun `follows the wall clock once it moves ahead`() {
        var now = 1_000L
        val clock = MonotonicClock { now }

        repeat(5) { clock.next() }
        now = 9_999L

        assertEquals(9_999L, clock.next())
    }

    /** 実時刻が巻き戻っても、発行済みの値より前には戻らない。 */
    @Test
    fun `never goes backwards when the wall clock jumps back`() {
        var now = 5_000L
        val clock = MonotonicClock { now }
        val first = clock.next()

        now = 1_000L
        val second = clock.next()

        assertTrue("second=$second first=$first", second > first)
    }
}
