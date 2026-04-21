package com.digikhata.ui.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Client
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerPickerViewModel @Inject constructor(
    repo: DigiRepository,
    active: ActiveBookHolder
) : ViewModel() {
    val clients: StateFlow<List<Client>> = active.id.flatMapLatest { bid ->
        if (bid == null) flowOf(emptyList()) else repo.clients(bid, 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
