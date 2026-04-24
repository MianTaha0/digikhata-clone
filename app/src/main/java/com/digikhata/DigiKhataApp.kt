package com.digikhata

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.digikhata.data.auth.AuthRepository
import com.digikhata.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DigiKhataApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var authRepo: AuthRepository
    @Inject lateinit var syncScheduler: SyncScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Phase 3b.2: drain the pending sync queue whenever a user is signed in.
        appScope.launch {
            authRepo.currentUser
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid != null) syncScheduler.schedulePush()
                }
        }
    }
}
