package com.digikhata.domain

import com.digikhata.data.entity.StaffAttendance
import com.digikhata.domain.model.AttendanceStatus
import java.util.Calendar

data class MonthSummary(
    val present: Int,
    val absent: Int,
    val halfDay: Int,
    val leave: Int,
    val weekOff: Int,
    val marked: Int,
    val daysInMonth: Int,
    val earned: Double
)

object AttendanceCalc {
    fun summarize(
        records: List<StaffAttendance>,
        monthlySalary: Double,
        year: Int,
        monthZeroBased: Int
    ): MonthSummary {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val inMonth = records.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == monthZeroBased
        }

        var present = 0
        var absent = 0
        var halfDay = 0
        var leave = 0
        var weekOff = 0
        var fractionSum = 0.0

        inMonth.forEach { r ->
            val s = AttendanceStatus.fromKey(r.status)
            when (s) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.ABSENT -> absent++
                AttendanceStatus.HALF_DAY -> halfDay++
                AttendanceStatus.LEAVE -> leave++
                AttendanceStatus.WEEK_OFF -> weekOff++
            }
            fractionSum += s.fraction
        }

        val perDay = if (daysInMonth == 0) 0.0 else monthlySalary / daysInMonth
        val earned = perDay * fractionSum

        return MonthSummary(
            present = present,
            absent = absent,
            halfDay = halfDay,
            leave = leave,
            weekOff = weekOff,
            marked = inMonth.size,
            daysInMonth = daysInMonth,
            earned = earned
        )
    }
}
