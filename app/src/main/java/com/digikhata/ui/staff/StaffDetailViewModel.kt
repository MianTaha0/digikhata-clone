package com.digikhata.ui.staff

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Staff
import com.digikhata.data.entity.StaffPayment
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StaffDetailViewModel @Inject constructor(
    private val repo: DigiRepository,
    active: ActiveBookHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val staffId: Long = savedStateHandle.get<Long>("staffId") ?: 0L

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

    val staff: StateFlow<Staff?> = repo.getStaff(staffId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val payments: StateFlow<List<StaffPayment>> = repo.staffPayments(staffId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val paidThisMonth: StateFlow<Double> = repo.paidInRange(staffId, monthRange.first, monthRange.second)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    suspend fun delete(s: Staff) {
        repo.deleteStaff(s)
    }

    suspend fun addPayment(amount: Double, date: Long, note: String?) {
        repo.addStaffPayment(
            StaffPayment(
                staffId = staffId,
                amount = amount,
                paymentDate = date,
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePayment(p: StaffPayment) {
        repo.deleteStaffPayment(p)
    }
}
