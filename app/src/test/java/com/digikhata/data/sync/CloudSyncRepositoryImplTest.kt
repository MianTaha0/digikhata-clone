package com.digikhata.data.sync

import com.digikhata.data.dao.SyncOpDao
import com.digikhata.data.entity.SyncOp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncRepositoryImplTest {

    private class FakeDao : SyncOpDao {
        val rows = mutableListOf<SyncOp>()
        private val count = MutableStateFlow(0)
        private var nextId = 1L

        override suspend fun peek(limit: Int): List<SyncOp> =
            rows.sortedBy { it.createdAt }.take(limit)

        override suspend fun enqueue(op: SyncOp): Long {
            val id = nextId++
            rows += op.copy(id = id)
            count.value = rows.size
            return id
        }

        override suspend fun ack(id: Long) {
            rows.removeAll { it.id == id }
            count.value = rows.size
        }

        override suspend fun bumpFailure(id: Long, err: String) {
            val idx = rows.indexOfFirst { it.id == id }
            if (idx >= 0) rows[idx] = rows[idx].copy(
                attempts = rows[idx].attempts + 1,
                lastError = err
            )
        }

        override fun observePendingCount() = count

        override suspend fun countNow(): Int = rows.size
    }

    private class FakeScheduler : PushTrigger {
        var scheduled = 0
        override fun schedulePush() { scheduled += 1 }
    }

    @Test
    fun `upsert enqueues row with serialized payload and triggers schedule`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = CloudSyncRepositoryImpl(dao, scheduler)

        val payload = mapOf("id" to 7L, "name" to "Ali")
        repo.onUpsert(businessId = 42L, collection = "clients", docId = "7", payload = payload)

        assertEquals(1, dao.rows.size)
        val row = dao.rows.single()
        assertEquals(42L, row.businessId)
        assertEquals("clients", row.collection)
        assertEquals("7", row.docId)
        assertEquals("UPSERT", row.opType)
        assertTrue(row.payloadJson.contains("Ali"))
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun `delete enqueues row with empty payload`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = CloudSyncRepositoryImpl(dao, scheduler)

        repo.onDelete(businessId = 1L, collection = "products", docId = "99")

        val row = dao.rows.single()
        assertEquals("DELETE", row.opType)
        assertEquals("", row.payloadJson)
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun `business ops allow null businessId`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = CloudSyncRepositoryImpl(dao, scheduler)

        repo.onUpsert(null, "businesses", "5", mapOf("id" to 5L, "name" to "Kirana Shop"))

        val row = dao.rows.single()
        assertEquals(null, row.businessId)
        assertEquals("businesses", row.collection)
    }

    @Test
    fun `pendingCount flows through from dao`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = CloudSyncRepositoryImpl(dao, scheduler)

        assertEquals(0, repo.pendingCount.first())
        repo.onUpsert(null, "businesses", "1", mapOf("id" to 1L))
        assertEquals(1, repo.pendingCount.first())
        repo.onUpsert(null, "businesses", "2", mapOf("id" to 2L))
        assertEquals(2, repo.pendingCount.first())
    }
}
