package com.digikhata.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.digikhata.data.reminders.DueInvoiceWorker
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

    fun schedulePull() {
        val work = OneTimeWorkRequestBuilder<PullWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork(PULL_WORK_NAME, ExistingWorkPolicy.KEEP, work)
    }

    fun schedulePeriodicPull() {
        val work = PeriodicWorkRequestBuilder<PullWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(
                PERIODIC_PULL_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
    }

    /** Phase 4a.1: daily invoice due-reminder scan. */
    fun scheduleDueReminders() {
        val work = PeriodicWorkRequestBuilder<DueInvoiceWorker>(1, TimeUnit.DAYS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(
                DUE_REMINDERS_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
    }

    /** One-shot scan — useful on book switch or after a sync pull. */
    fun scheduleDueRemindersOnce() {
        val work = OneTimeWorkRequestBuilder<DueInvoiceWorker>().build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork(
                DUE_REMINDERS_ONCE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                work
            )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "digi-push"
        const val PULL_WORK_NAME = "digikhata_pull_once"
        const val PERIODIC_PULL_WORK_NAME = "digikhata_pull"
        const val DUE_REMINDERS_WORK_NAME = "digikhata_due_reminders"
        const val DUE_REMINDERS_ONCE_WORK_NAME = "digikhata_due_reminders_once"
    }
}
