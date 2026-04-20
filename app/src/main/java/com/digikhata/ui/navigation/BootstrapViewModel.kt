package com.digikhata.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BootstrapViewModel @Inject constructor(
    repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {

    val needsBook: StateFlow<Boolean?> = repo.businesses
        .onEach { list ->
            if (list.isNotEmpty() && active.id.value == null) {
                active.set(list.first().id)
            }
        }
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
