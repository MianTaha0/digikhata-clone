package com.digikhata.ui.home

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
class HomeViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    val business: StateFlow<Business?> = active.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val clients: StateFlow<List<ClientBalance>> = active.id
        .flatMapLatest { bid ->
            if (bid == null) flowOf(emptyList())
            else repo.clients(bid, 0).flatMapLatest { list ->
                if (list.isEmpty()) flowOf(emptyList())
                else clientBalancesFlow(list)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<BusinessTotals> = clients
        .map { list ->
            val get = list.sumOf { if (it.balance > 0) it.balance else 0.0 }
            val give = list.sumOf { if (it.balance < 0) -it.balance else 0.0 }
            BusinessTotals(totalWillGet = get, totalWillGive = give)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessTotals(0.0, 0.0))

    private fun clientBalancesFlow(list: List<Client>): Flow<List<ClientBalance>> {
        val flows: List<Flow<ClientBalance>> = list.map { c ->
            repo.balanceForClient(c.id).map { bal -> ClientBalance(c, bal) }
        }
        return combine(flows) { it.toList() }
    }

    fun upsertClient(client: Client) {
        viewModelScope.launch {
            val bid = active.id.value ?: return@launch
            repo.upsertClient(client.copy(businessId = bid))
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch { repo.deleteClient(client) }
    }

    fun togglePin(client: Client) {
        viewModelScope.launch { repo.upsertClient(client.copy(isPinned = !client.isPinned)) }
    }

    fun toggleArchive(client: Client) {
        viewModelScope.launch { repo.upsertClient(client.copy(isArchived = !client.isArchived)) }
    }
}
