package com.digikhata.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digikhata.data.auth.AuthRepository
import com.digikhata.data.dao.SyncOpDao
import com.digikhata.data.entity.SyncOp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * Phase 3b.2: drains the `sync_ops` queue to Firestore under
 * `users/{uid}/businesses/{bizId}/{collection}/{docId}`.
 *
 * - Signed out: returns success, ops stay queued for next sign-in.
 * - Per-op failure: bumps attempts + lastError, moves on.
 * - Network/whole-worker failure: Result.retry() so WorkManager backoff applies.
 * - If more ops remain after the batch, re-schedules itself.
 */
@HiltWorker
class PushWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val syncOpDao: SyncOpDao,
    private val auth: AuthRepository,
    private val scheduler: SyncScheduler,
    private val firestoreProvider: FirestoreProvider
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser.value?.uid
            ?: return Result.success() // signed out — leave ops in queue

        val firestore: FirebaseFirestore = try {
            firestoreProvider.get()
        } catch (t: Throwable) {
            // Firestore is not initialized (no google-services.json). Retry later.
            return Result.retry()
        }

        val batch = try {
            syncOpDao.peek(BATCH_SIZE)
        } catch (t: Throwable) {
            return Result.retry()
        }
        if (batch.isEmpty()) return Result.success()

        var hadNetworkError = false

        for (op in batch) {
            try {
                pushOne(firestore, uid, op)
                syncOpDao.ack(op.id)
            } catch (t: Throwable) {
                val msg = t.message ?: t::class.java.simpleName
                runCatching { syncOpDao.bumpFailure(op.id, msg) }
                // Treat most errors as transient network errors to trigger backoff.
                if (isLikelyNetwork(t)) hadNetworkError = true
            }
        }

        if (hadNetworkError) return Result.retry()

        val remaining = runCatching { syncOpDao.countNow() }.getOrDefault(0)
        if (remaining > 0) scheduler.schedulePush()
        return Result.success()
    }

    private suspend fun pushOne(firestore: FirebaseFirestore, uid: String, op: SyncOp) {
        val docRef = if (op.businessId == null) {
            // Business entity itself: users/{uid}/businesses/{docId}
            firestore.collection("users").document(uid)
                .collection(op.collection).document(op.docId)
        } else {
            firestore.collection("users").document(uid)
                .collection("businesses").document(op.businessId.toString())
                .collection(op.collection).document(op.docId)
        }

        when (op.opType) {
            CloudSyncRepositoryImpl.OP_UPSERT -> {
                val map = EntitySerializer.toMap(op.payloadJson).toMutableMap()
                map["serverUpdatedAt"] = FieldValue.serverTimestamp()
                docRef.set(map).await()
            }
            CloudSyncRepositoryImpl.OP_DELETE -> {
                docRef.delete().await()
            }
            else -> {
                // Unknown op — drop it to avoid infinite retries.
            }
        }
    }

    private fun isLikelyNetwork(t: Throwable): Boolean {
        val name = t::class.java.simpleName.lowercase()
        val msg = (t.message ?: "").lowercase()
        return "network" in name || "unavailable" in msg || "timeout" in msg || "network" in msg
    }

    companion object {
        private const val BATCH_SIZE = 50
    }
}
