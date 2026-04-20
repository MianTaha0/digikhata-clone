package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.Client
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client): Long

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("SELECT * FROM clients WHERE id = :id")
    fun getById(id: Long): Flow<Client?>

    @Query("SELECT * FROM clients WHERE businessId = :businessId AND type = :type AND isArchived = 0 ORDER BY isPinned DESC, name ASC")
    fun getByBusinessAndType(businessId: Long, type: Int): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE businessId = :businessId AND type = :type AND isArchived = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun search(businessId: Long, type: Int, query: String): Flow<List<Client>>
}
