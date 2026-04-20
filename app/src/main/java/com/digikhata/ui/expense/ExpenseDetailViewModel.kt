package com.digikhata.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L

    val entry: StateFlow<ExpenseEntry?> = repo.getExpense(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    suspend fun delete(entry: ExpenseEntry) {
        repo.deleteExpense(entry)
    }
}
