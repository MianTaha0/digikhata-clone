package com.digikhata.data.sync

/**
 * Phase 3b.3: pure conflict-resolution helpers used by [PullEngine].
 *
 * Keeping the decision logic in a plain function (no Firestore, no Room) makes
 * it trivially unit-testable. The engine is responsible for loading local/remote
 * rows and applying the returned [MergeDecision].
 */

/** Minimal snapshot of what we need to reason about a local row. */
data class LocalSnapshot(val updatedAt: Long, val deletedAt: Long?)

/** Minimal snapshot of a remote Firestore doc. `data` is the raw document map. */
data class RemoteSnapshot(
    val updatedAt: Long,
    val deletedAt: Long?,
    val data: Map<String, Any?>
)

sealed class MergeDecision {
    /** Local row is newer than remote — do nothing, push worker will handle it. */
    object SkipLocalNewer : MergeDecision()
    /** No local row exists yet — insert the remote payload. */
    data class InsertNew(val data: Map<String, Any?>) : MergeDecision()
    /** Local row exists and is older (or equal) — overwrite with remote payload. */
    data class ApplyRemote(val data: Map<String, Any?>) : MergeDecision()
    /** Remote doc is a tombstone — soft-delete the local row. */
    data class ApplyRemoteDelete(val data: Map<String, Any?>) : MergeDecision()
}

fun decideMerge(local: LocalSnapshot?, remote: RemoteSnapshot): MergeDecision {
    // Remote deleted always wins over older-or-equal local state; LWW applies to tombstones too.
    if (remote.deletedAt != null) {
        if (local != null && local.updatedAt > remote.updatedAt) return MergeDecision.SkipLocalNewer
        return MergeDecision.ApplyRemoteDelete(remote.data)
    }
    if (local == null) return MergeDecision.InsertNew(remote.data)
    // Ties go to remote so two devices writing at the exact same ms converge.
    return if (local.updatedAt > remote.updatedAt) {
        MergeDecision.SkipLocalNewer
    } else {
        MergeDecision.ApplyRemote(remote.data)
    }
}
