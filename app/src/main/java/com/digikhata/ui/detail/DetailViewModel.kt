package com.digikhata.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.TxEntity
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val clientId: Long = savedStateHandle.get<Long>("clientId") ?: 0L

    val client: StateFlow<Client?> = repo.getClient(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val transactions: StateFlow<List<TxEntity>> = repo.transactions(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val balance: StateFlow<Double> = repo.balanceForClient(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val business: StateFlow<Business?> = active.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addTransaction(amount: Double, type: Int, notes: String?, entryDate: Long, imagePath: String?) {
        viewModelScope.launch {
            val bid = client.value?.businessId ?: active.id.value ?: return@launch
            val tx = TxEntity(
                clientId = clientId,
                businessId = bid,
                amount = amount,
                type = type,
                notes = notes,
                entryDate = entryDate,
                imageLocalPath = imagePath
            )
            repo.addTransaction(tx, listOfNotNull(imagePath))
        }
    }

    fun updateTx(tx: TxEntity) { viewModelScope.launch { repo.updateTransaction(tx) } }
    fun deleteTx(tx: TxEntity) { viewModelScope.launch { repo.deleteTransaction(tx) } }
}
