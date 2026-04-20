package com.digikhata.ui.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.Client
import com.digikhata.domain.model.BusinessTotals
import com.digikhata.domain.model.ClientBalance
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    val business: StateFlow<Business?> = active.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val suppliers: StateFlow<List<ClientBalance>> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(emptyList())
            else repo.clients(bid, 1).flatMapLatest { list ->
                if (list.isEmpty()) flowOf(emptyList())
                else combine(list.map { c -> repo.balanceForClient(c.id).map { ClientBalance(c, it) } }) { it.toList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<BusinessTotals> = suppliers
        .map { list ->
            val get = list.sumOf { if (it.balance > 0) it.balance else 0.0 }
            val give = list.sumOf { if (it.balance < 0) -it.balance else 0.0 }
            BusinessTotals(totalWillGet = get, totalWillGive = give)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessTotals(0.0, 0.0))

    fun upsert(client: Client) {
        viewModelScope.launch {
            val bid = active.id.value ?: return@launch
            repo.upsertClient(client.copy(businessId = bid, type = 1))
        }
    }

    fun delete(client: Client) { viewModelScope.launch { repo.deleteClient(client) } }
    fun togglePin(c: Client) { viewModelScope.launch { repo.upsertClient(c.copy(isPinned = !c.isPinned)) } }
    fun toggleArchive(c: Client) { viewModelScope.launch { repo.upsertClient(c.copy(isArchived = !c.isArchived)) } }
}
