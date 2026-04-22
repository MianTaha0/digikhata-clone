package com.digikhata.ui.cash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CashFilterTest {

    private fun cal(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `ALL covers the entire timeline`() {
        val (start, end) = CashFilter.ALL.range(now = cal(2026, Calendar.APRIL, 22))
        assertEquals(0L, start)
        assertEquals(Long.MAX_VALUE, end)
    }

    @Test
    fun `TODAY starts at midnight and ends at end-of-day`() {
        val now = cal(2026, Calendar.APRIL, 22, 15, 30)
        val (start, end) = CashFilter.TODAY.range(now = now)

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(22, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))

        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(22, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun `WEEK starts on Monday`() {
        // April 22 2026 is a Wednesday
        val now = cal(2026, Calendar.APRIL, 22, 15, 0)
        val (start, _) = CashFilter.WEEK.range(now = now)
        val startCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = start
        }
        assertEquals(Calendar.MONDAY, startCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertTrue("start should precede now", start <= now)
    }

    @Test
    fun `MONTH starts on the first of the month`() {
        val now = cal(2026, Calendar.APRIL, 22, 15, 0)
        val (start, _) = CashFilter.MONTH.range(now = now)
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.APRIL, startCal.get(Calendar.MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `range is inclusive of now for non-ALL filters`() {
        val now = cal(2026, Calendar.APRIL, 22, 15, 0)
        for (f in listOf(CashFilter.TODAY, CashFilter.WEEK, CashFilter.MONTH)) {
            val (start, end) = f.range(now = now)
            assertTrue("$f: now should be in range", now in start..end)
        }
    }
}
