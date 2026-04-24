package com.digikhata.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digikhata.data.auth.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Phase 3b.3: pulls remote deltas into Room.
 *
 * - Signed out: success (no-op).
 * - Firestore not initialized: retry.
 * - Anything else: log and retry.
 */
@HiltWorker
class PullWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val auth: AuthRepository,
    private val pullEngine: PullEngine
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser.value?.uid ?: return Result.success()
        return pullEngine.pullOnce(uid).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
