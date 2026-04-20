package com.digikhata.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.data.entity.DigiNotification
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: DigiRepository
) : ViewModel() {
    val items: StateFlow<List<DigiNotification>> = repo.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unseenCount: StateFlow<Int> = repo.notifications
        .map { list -> list.count { !it.isSeen } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markSeen(id: Long) { viewModelScope.launch { repo.markNotificationSeen(id) } }

    fun add(notification: DigiNotification) { viewModelScope.launch { repo.addNotification(notification) } }
}
