package com.digikhata.domain

import com.digikhata.data.entity.StaffAttendance
import com.digikhata.domain.model.AttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AttendanceCalcTest {

    private fun dateMillis(year: Int, monthZeroBased: Int, day: Int): Long {
        val c = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return c.timeInMillis
    }

    private fun record(year: Int, month: Int, day: Int, status: AttendanceStatus): StaffAttendance =
        StaffAttendance(
            staffId = 1L,
            date = dateMillis(year, month, day),
            status = status.key
        )

    @Test
    fun emptyRecords() {
        val summary = AttendanceCalc.summarize(emptyList(), 12000.0, 2026, Calendar.APRIL)
        assertEquals(0, summary.present)
        assertEquals(0, summary.absent)
        assertEquals(0, summary.halfDay)
        assertEquals(0, summary.leave)
        assertEquals(0, summary.weekOff)
        assertEquals(0, summary.marked)
        assertEquals(30, summary.daysInMonth)
        assertEquals(0.0, summary.earned, 0.01)
    }

    @Test
    fun allPresentAprilEqualsSalary() {
        val records = (1..30).map { record(2026, Calendar.APRIL, it, AttendanceStatus.PRESENT) }
        val summary = AttendanceCalc.summarize(records, 12000.0, 2026, Calendar.APRIL)
        assertEquals(30, summary.present)
        assertEquals(12000.0, summary.earned, 0.01)
    }

    @Test
    fun mixedStatusesApril() {
        val records = buildList {
            (1..18).forEach { add(record(2026, Calendar.APRIL, it, AttendanceStatus.PRESENT)) }
            (19..20).forEach { add(record(2026, Calendar.APRIL, it, AttendanceStatus.ABSENT)) }
            add(record(2026, Calendar.APRIL, 21, AttendanceStatus.HALF_DAY))
        }
        val summary = AttendanceCalc.summarize(records, 12000.0, 2026, Calendar.APRIL)
        assertEquals(18, summary.present)
        assertEquals(2, summary.absent)
        assertEquals(1, summary.halfDay)
        assertEquals(7400.0, summary.earned, 0.01)
    }

    @Test
    fun weekOffAndLeaveContributeFullFraction() {
        val records = listOf(
            record(2026, Calendar.APRIL, 1, AttendanceStatus.LEAVE),
            record(2026, Calendar.APRIL, 2, AttendanceStatus.WEEK_OFF)
        )
        val summary = AttendanceCalc.summarize(records, 3000.0, 2026, Calendar.APRIL)
        // perDay = 100, earned = 200
        assertEquals(200.0, summary.earned, 0.01)
        assertEquals(1, summary.leave)
        assertEquals(1, summary.weekOff)
    }

    @Test
    fun wrongMonthRecordsIgnored() {
        val records = listOf(
            record(2026, Calendar.MARCH, 31, AttendanceStatus.PRESENT),
            record(2026, Calendar.APRIL, 1, AttendanceStatus.PRESENT)
        )
        val summary = AttendanceCalc.summarize(records, 3000.0, 2026, Calendar.APRIL)
        assertEquals(1, summary.present)
        assertEquals(1, summary.marked)
        assertEquals(100.0, summary.earned, 0.01)
    }

    @Test
    fun february2025Has28Days() {
        val records = (1..28).map { record(2025, Calendar.FEBRUARY, it, AttendanceStatus.PRESENT) }
        val summary = AttendanceCalc.summarize(records, 2800.0, 2025, Calendar.FEBRUARY)
        assertEquals(28, summary.daysInMonth)
        assertEquals(2800.0, summary.earned, 0.01)
    }

    @Test
    fun zeroSalaryYieldsZeroEarned() {
        val records = (1..30).map { record(2026, Calendar.APRIL, it, AttendanceStatus.PRESENT) }
        val summary = AttendanceCalc.summarize(records, 0.0, 2026, Calendar.APRIL)
        assertEquals(0.0, summary.earned, 0.01)
    }
}
