package com.digikhata.ui.drawer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.auth.AuthRepository
import com.digikhata.data.auth.DigiUser
import com.digikhata.data.entity.Business
import com.digikhata.data.sync.CloudSyncRepository
import com.digikhata.data.sync.PullEngineImpl
import com.digikhata.domain.repository.DigiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    repo: DigiRepository,
    private val active: ActiveBookHolder,
    authRepo: AuthRepository,
    cloudSync: CloudSyncRepository,
    @ApplicationContext ctx: Context
) : ViewModel() {
    private val syncPrefs = ctx.getSharedPreferences("digikhata_sync_prefs", Context.MODE_PRIVATE)
    val businesses: StateFlow<List<Business>> = repo.businesses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeId: StateFlow<Long?> = active.id

    val currentUser: StateFlow<DigiUser?> = authRepo.currentUser

    val pendingSyncCount: StateFlow<Int> = cloudSync.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val lastPullAt: StateFlow<Long> = callbackFlow {
        val key = PullEngineImpl.KEY_LAST_PULL_AT
        trySend(syncPrefs.getLong(key, 0L))
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (k == key) trySend(syncPrefs.getLong(key, 0L))
        }
        syncPrefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { syncPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setActive(id: Long) = active.set(id)
}
