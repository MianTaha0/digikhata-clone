package com.digikhata.ui.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Client
import com.digikhata.domain.repository.DigiRepository
import com.digikhata.util.InvoiceCalc
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
class InvoiceListViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    val filter: MutableStateFlow<InvoiceListFilter> = MutableStateFlow(InvoiceListFilter.ALL)

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    val prefix: StateFlow<String> = active.active
        .map { it?.invoicePrefix ?: "INV-" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "INV-")

    val activeBookId: StateFlow<Long?> = active.id
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val clientsByBook: StateFlow<Map<Long, Client>> =
        active.id.flatMapLatest { bid ->
            if (bid == null) flowOf(emptyList<Client>())
            else combine(
                repo.clients(bid, 0),
                repo.clients(bid, 1)
            ) { customers, suppliers -> customers + suppliers }
        }
            .map { list -> list.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val invoices = active.id.flatMapLatest { bid ->
        if (bid == null) flowOf(emptyList()) else repo.invoices(bid)
    }

    val cards: StateFlow<List<InvoiceCardData>> =
        combine(invoices, clientsByBook, filter) { list, clients, f ->
            Triple(list, clients, f)
        }
            .flatMapLatest { (list, clients, f) ->
                // For each invoice we need items to compute totals.
                if (list.isEmpty()) flowOf(emptyList<InvoiceCardData>())
                else {
                    val flows = list.map { inv -> repo.invoiceItems(inv.id) }
                    combine(flows) { allItems ->
                        list.mapIndexed { idx, inv ->
                            val items = allItems[idx]
                            InvoiceCardData(
                                invoice = inv,
                                customerName = clients[inv.customerId]?.name ?: "Customer",
                                totals = InvoiceCalc.compute(inv, items)
                            )
                        }.filter { card ->
                            when (f) {
                                InvoiceListFilter.ALL -> true
                                InvoiceListFilter.PENDING -> card.totals.status.name == "PENDING"
                                InvoiceListFilter.PARTIAL -> card.totals.status.name == "PARTIAL"
                                InvoiceListFilter.PAID -> card.totals.status.name == "PAID"
                            }
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(f: InvoiceListFilter) { filter.value = f }
}
