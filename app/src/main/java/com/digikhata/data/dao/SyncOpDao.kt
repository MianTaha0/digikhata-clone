package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.digikhata.data.entity.SyncOp
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOpDao {
    @Query("SELECT * FROM sync_ops ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peek(limit: Int): List<SyncOp>

    @Insert
    suspend fun enqueue(op: SyncOp): Long

    @Query("DELETE FROM sync_ops WHERE id = :id")
    suspend fun ack(id: Long)

    @Query("UPDATE sync_ops SET attempts = attempts + 1, lastError = :err WHERE id = :id")
    suspend fun bumpFailure(id: Long, err: String)

    @Query("SELECT COUNT(*) FROM sync_ops")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_ops")
    suspend fun countNow(): Int
}
