package com.digikhata.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.Client
import com.digikhata.domain.ReportsCalc
import com.digikhata.domain.model.ClientBalance
import com.digikhata.domain.model.ReportsSummary
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class ReportsViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    val business: StateFlow<Business?> = active.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val summary: StateFlow<ReportsSummary> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(ReportsSummary.EMPTY)
            else summaryFlow(bid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsSummary.EMPTY)

    private fun summaryFlow(bid: Long): Flow<ReportsSummary> {
        // Cash entries: pull "all-time" slice by using a very wide window
        val cashFlow = repo.cashEntries(bid, 0L, Long.MAX_VALUE)
        val expenseFlow = repo.expenses(bid, 0L, Long.MAX_VALUE)
        val invoicesFlow = repo.invoices(bid)
        val clientsFlow = combine(
            repo.clients(bid, 0),
            repo.clients(bid, 1)
        ) { a, b -> a + b }

        val clientBalancesFlow: Flow<List<ClientBalance>> =
            clientsFlow.flatMapLatest { list ->
                if (list.isEmpty()) flowOf(emptyList())
                else combine(list.map { c -> balanceFlow(c) }) { it.toList() }
            }

        return combine(
            invoicesFlow,
            expenseFlow,
            cashFlow,
            clientBalancesFlow
        ) { inv, exp, cash, bals ->
            ReportsCalc.compute(inv, exp, cash, bals)
        }
    }

    private fun balanceFlow(c: Client): Flow<ClientBalance> =
        repo.balanceForClient(c.id).map { bal -> ClientBalance(c, bal) }
}
