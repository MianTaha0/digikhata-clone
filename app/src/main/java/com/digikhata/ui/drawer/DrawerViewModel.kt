package com.digikhata.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Business
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    repo: DigiRepository,
    private val active: ActiveBookHolder
) : ViewModel() {
    val businesses: StateFlow<List<Business>> = repo.businesses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeId: StateFlow<Long?> = active.id

    fun setActive(id: Long) = active.set(id)
}
