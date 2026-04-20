package com.digikhata.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    val filter: MutableStateFlow<ExpenseFilter> = MutableStateFlow(ExpenseFilter.MONTH)

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    val activeBookId: StateFlow<Long?> = active.id
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val entries: StateFlow<List<ExpenseEntry>> =
        combine(active.id, filter) { bid, f -> bid to f }
            .flatMapLatest { (bid, f) ->
                if (bid == null) flowOf(emptyList()) else {
                    val (from, to) = f.range()
                    repo.expenses(bid, from, to)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val total: StateFlow<Double> =
        combine(active.id, filter) { bid, f -> bid to f }
            .flatMapLatest { (bid, f) ->
                if (bid == null) flowOf(0.0) else {
                    val (from, to) = f.range()
                    repo.expenseTotal(bid, from, to)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun setFilter(f: ExpenseFilter) { filter.value = f }
}
