package com.digikhata.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PullMergeTest {

    private fun remote(updatedAt: Long, deletedAt: Long? = null, extra: Map<String, Any?> = emptyMap()) =
        RemoteSnapshot(
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            data = extra + mapOf("updatedAt" to updatedAt, "deletedAt" to deletedAt)
        )

    @Test
    fun `no local row results in InsertNew`() {
        val decision = decideMerge(null, remote(updatedAt = 1000L))
        assertTrue(decision is MergeDecision.InsertNew)
    }

    @Test
    fun `local older than remote applies remote`() {
        val local = LocalSnapshot(updatedAt = 500L, deletedAt = null)
        val decision = decideMerge(local, remote(updatedAt = 1000L))
        assertTrue(decision is MergeDecision.ApplyRemote)
    }

    @Test
    fun `local newer than remote is skipped`() {
        val local = LocalSnapshot(updatedAt = 2000L, deletedAt = null)
        val decision = decideMerge(local, remote(updatedAt = 1000L))
        assertEquals(MergeDecision.SkipLocalNewer, decision)
    }

    @Test
    fun `remote tombstone is applied when local is older`() {
        val local = LocalSnapshot(updatedAt = 500L, deletedAt = null)
        val decision = decideMerge(local, remote(updatedAt = 1000L, deletedAt = 1000L))
        assertTrue(decision is MergeDecision.ApplyRemoteDelete)
    }

    @Test
    fun `remote tombstone loses to strictly newer local write`() {
        val local = LocalSnapshot(updatedAt = 2000L, deletedAt = null)
        val decision = decideMerge(local, remote(updatedAt = 1000L, deletedAt = 1000L))
        assertEquals(MergeDecision.SkipLocalNewer, decision)
    }

    @Test
    fun `remote tombstone applies when there is no local row`() {
        val decision = decideMerge(null, remote(updatedAt = 1000L, deletedAt = 1000L))
        assertTrue(decision is MergeDecision.ApplyRemoteDelete)
    }

    @Test
    fun `equal updatedAt ties break to remote`() {
        val local = LocalSnapshot(updatedAt = 1000L, deletedAt = null)
        val decision = decideMerge(local, remote(updatedAt = 1000L))
        assertTrue(decision is MergeDecision.ApplyRemote)
    }

    @Test
    fun `insert decision carries remote data payload`() {
        val decision = decideMerge(null, remote(updatedAt = 1000L, extra = mapOf("name" to "Ali")))
        val insert = decision as MergeDecision.InsertNew
        assertEquals("Ali", insert.data["name"])
    }

    @Test
    fun `apply decision carries remote data payload`() {
        val local = LocalSnapshot(updatedAt = 500L, deletedAt = null)
        val decision = decideMerge(local, remote(updatedAt = 1000L, extra = mapOf("name" to "Zara")))
        val apply = decision as MergeDecision.ApplyRemote
        assertEquals("Zara", apply.data["name"])
    }
}
