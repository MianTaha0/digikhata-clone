package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.TxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TxEntity): Long

    @Update
    suspend fun update(tx: TxEntity)

    @Delete
    suspend fun delete(tx: TxEntity)

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Long): Flow<TxEntity?>

    @Query("SELECT * FROM transactions WHERE clientId = :clientId AND deletedAt IS NULL ORDER BY entryDate DESC, id DESC")
    fun getByClient(clientId: Long): Flow<List<TxEntity>>

    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM transactions WHERE clientId = :clientId AND type = 0 AND deletedAt IS NULL), 0.0
        ) - COALESCE(
            (SELECT SUM(amount) FROM transactions WHERE clientId = :clientId AND type = 1 AND deletedAt IS NULL), 0.0
        )
    """)
    fun balanceForClient(clientId: Long): Flow<Double?>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE businessId = :businessId AND type = 0 AND deletedAt IS NULL")
    fun totalGaveForBusiness(businessId: Long): Flow<Double?>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE businessId = :businessId AND type = 1 AND deletedAt IS NULL")
    fun totalGotForBusiness(businessId: Long): Flow<Double?>
}
