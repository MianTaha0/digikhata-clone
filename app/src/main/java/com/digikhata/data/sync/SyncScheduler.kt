package com.digikhata.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 3b.2: thin wrapper around WorkManager to schedule cloud pushes.
 * Uses unique work with APPEND_OR_REPLACE so enqueueing many ops in a burst
 * does not spawn many concurrent workers.
 */
/** Minimal surface CloudSyncRepositoryImpl needs — makes it unit-testable without WorkManager. */
interface PushTrigger {
    fun schedulePush()
}

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context
) : PushTrigger {
    override fun schedulePush() {
        val work = OneTimeWorkRequestBuilder<PushWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "digi-push"
    }
}
