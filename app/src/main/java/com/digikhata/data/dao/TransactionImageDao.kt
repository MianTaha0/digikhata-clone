package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digikhata.data.entity.TransactionImage
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: TransactionImage): Long

    @Query("SELECT * FROM transaction_images WHERE transactionId = :txId ORDER BY createdAt ASC")
    fun getByTransaction(txId: Long): Flow<List<TransactionImage>>

    @Query("DELETE FROM transaction_images WHERE transactionId = :txId")
    suspend fun deleteByTransaction(txId: Long)
}
