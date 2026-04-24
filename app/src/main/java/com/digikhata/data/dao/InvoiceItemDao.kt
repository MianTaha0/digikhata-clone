package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digikhata.data.entity.InvoiceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :id")
    suspend fun deleteByInvoice(id: Long)

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :id AND deletedAt IS NULL ORDER BY sortOrder ASC, id ASC")
    fun getByInvoice(id: Long): Flow<List<InvoiceItem>>
}
