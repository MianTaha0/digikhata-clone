package com.digikhata.ui.staff

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.data.entity.Staff
import com.digikhata.data.entity.StaffAttendance
import com.digikhata.domain.AttendanceCalc
import com.digikhata.domain.MonthSummary
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class YearMonth(val year: Int, val month: Int) {
    fun prev(): YearMonth {
        val c = Calendar.getInstance().apply {
            clear(); set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        return YearMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }

    fun next(): YearMonth {
        val c = Calendar.getInstance().apply {
            clear(); set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
        }
        return YearMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }

    fun startMillis(): Long = Calendar.getInstance().apply {
        clear(); set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    fun endMillis(): Long = Calendar.getInstance().apply {
        clear(); set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }.timeInMillis

    fun daysInMonth(): Int = Calendar.getInstance().apply {
        clear(); set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    companion object {
        fun now(): YearMonth {
            val c = Calendar.getInstance()
            return YearMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AttendanceCalendarViewModel @Inject constructor(
    private val repo: DigiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val staffId: Long = savedStateHandle.get<Long>("staffId") ?: 0L

    val visibleMonth: MutableStateFlow<YearMonth> = MutableStateFlow(YearMonth.now())

    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors

    val staff: StateFlow<Staff?> = repo.getStaff(staffId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val records: StateFlow<List<StaffAttendance>> = visibleMonth
        .flatMapLatest { ym -> repo.observeAttendance(staffId, ym.startMillis(), ym.endMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<MonthSummary> = combine(records, visibleMonth, staff) { recs, ym, s ->
        AttendanceCalc.summarize(recs, s?.monthlySalary ?: 0.0, ym.year, ym.month)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonthSummary(0, 0, 0, 0, 0, 0, YearMonth.now().daysInMonth(), 0.0)
    )

    fun prevMonth() { visibleMonth.value = visibleMonth.value.prev() }
    fun nextMonth() { visibleMonth.value = visibleMonth.value.next() }

    fun upsert(dateMillis: Long, status: String, notes: String?) {
        viewModelScope.launch {
            try {
                val existing = (records.value).firstOrNull { it.date == dateMillis }
                val record = existing?.copy(status = status, notes = notes)
                    ?: StaffAttendance(
                        staffId = staffId,
                        date = dateMillis,
                        status = status,
                        notes = notes
                    )
                repo.upsertAttendance(record)
            } catch (t: Throwable) {
                _errors.emit(t.message ?: "Failed to save attendance")
            }
        }
    }

    fun clear(dateMillis: Long) {
        viewModelScope.launch {
            try {
                repo.clearAttendance(staffId, dateMillis)
            } catch (t: Throwable) {
                _errors.emit(t.message ?: "Failed to clear attendance")
            }
        }
    }
}
