package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.DigiNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: DigiNotification): Long

    @Update
    suspend fun update(notification: DigiNotification)

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DigiNotification>>

    @Query("SELECT * FROM notifications WHERE isSeen = 0 ORDER BY createdAt DESC")
    fun getUnseen(): Flow<List<DigiNotification>>

    @Query("UPDATE notifications SET isSeen = 1 WHERE id = :id")
    suspend fun markSeen(id: Long)
}
