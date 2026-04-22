package com.digikhata.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Staff
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StaffListViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    private val monthRange: Pair<Long, Long> = run {
        val now = Calendar.getInstance()
        val start = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        start to end
    }

    val activeBookId: StateFlow<Long?> = active.id
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    val staff: StateFlow<List<Staff>> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(emptyList()) else repo.staffList(bid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val staffCount: StateFlow<Int> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(0) else repo.staffCount(bid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalPayroll: StateFlow<Double> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(0.0) else repo.totalMonthlyPayroll(bid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val paidThisMonth: StateFlow<Double> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(0.0)
            else repo.paidThisMonthForBusiness(bid, monthRange.first, monthRange.second)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val paidThisMonthByStaff: StateFlow<Map<Long, Double>> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(emptyMap())
            else repo.paidByStaffInRange(bid, monthRange.first, monthRange.second)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
