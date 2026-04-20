package com.digikhata.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.CashEntry
import com.digikhata.domain.model.CashTotals
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
class CashRegisterViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    val filter: MutableStateFlow<CashFilter> = MutableStateFlow(CashFilter.MONTH)

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    val activeBookId: StateFlow<Long?> = active.id
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val entries: StateFlow<List<CashEntry>> =
        combine(active.id, filter) { bid, f -> bid to f }
            .flatMapLatest { (bid, f) ->
                if (bid == null) flowOf(emptyList()) else {
                    val (from, to) = f.range()
                    repo.cashEntries(bid, from, to)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<CashTotals> =
        combine(active.id, filter) { bid, f -> bid to f }
            .flatMapLatest { (bid, f) ->
                if (bid == null) flowOf(CashTotals()) else {
                    val (from, to) = f.range()
                    repo.cashTotals(bid, from, to)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashTotals())

    fun setFilter(f: CashFilter) { filter.value = f }
}
