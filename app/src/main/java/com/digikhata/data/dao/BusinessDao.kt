package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.Business
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(business: Business): Long

    @Update
    suspend fun update(business: Business)

    @Delete
    suspend fun delete(business: Business)

    @Query("SELECT * FROM businesses WHERE id = :id")
    fun getById(id: Long): Flow<Business?>

    @Query("SELECT * FROM businesses ORDER BY createdAt ASC")
    fun getAll(): Flow<List<Business>>

    @Query("SELECT COUNT(*) FROM businesses")
    fun count(): Flow<Int>
}
