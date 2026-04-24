package com.digikhata.data.sync

import com.digikhata.data.dao.SyncOpDao
import com.digikhata.data.entity.SyncOp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 3b.2: façade that local repositories call after every successful write.
 * Enqueues a pending cloud-sync op and kicks the WorkManager-based pusher.
 *
 * One-way push only; Phase 3b.3 will add pull + conflict resolution.
 */
interface CloudSyncRepository {
    suspend fun onUpsert(businessId: Long?, collection: String, docId: String, payload: Any)
    suspend fun onDelete(businessId: Long?, collection: String, docId: String)
    val pendingCount: Flow<Int>
}

@Singleton
class CloudSyncRepositoryImpl @Inject constructor(
    private val dao: SyncOpDao,
    private val scheduler: PushTrigger
) : CloudSyncRepository {

    override val pendingCount: Flow<Int> = dao.observePendingCount()

    override suspend fun onUpsert(
        businessId: Long?,
        collection: String,
        docId: String,
        payload: Any
    ) {
        val json = EntitySerializer.toJson(payload)
        dao.enqueue(
            SyncOp(
                businessId = businessId,
                collection = collection,
                docId = docId,
                opType = OP_UPSERT,
                payloadJson = json
            )
        )
        scheduler.schedulePush()
    }

    override suspend fun onDelete(businessId: Long?, collection: String, docId: String) {
        dao.enqueue(
            SyncOp(
                businessId = businessId,
                collection = collection,
                docId = docId,
                opType = OP_DELETE,
                payloadJson = ""
            )
        )
        scheduler.schedulePush()
    }

    companion object {
        const val OP_UPSERT = "UPSERT"
        const val OP_DELETE = "DELETE"
    }
}
