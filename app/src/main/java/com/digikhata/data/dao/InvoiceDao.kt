package com.digikhata.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digikhata.data.entity.Invoice
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(inv: Invoice): Long

    @Update
    suspend fun updateInvoice(inv: Invoice)

    @Delete
    suspend fun deleteInvoice(inv: Invoice)

    @Query("SELECT * FROM invoices WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Long): Flow<Invoice?>

    @Query("SELECT * FROM invoices WHERE businessId = :bid AND deletedAt IS NULL ORDER BY issueDate DESC, id DESC")
    fun getByBusiness(bid: Long): Flow<List<Invoice>>

    @Query("SELECT COALESCE(MAX(sequenceNumber), 0) + 1 FROM invoices WHERE businessId = :bid")
    suspend fun nextSequenceNumber(bid: Long): Int

    @Query(
        """
        SELECT DISTINCT ii.name FROM invoice_items ii
        JOIN invoices i ON i.id = ii.invoiceId
        WHERE i.businessId = :bid AND i.deletedAt IS NULL AND ii.deletedAt IS NULL
        ORDER BY ii.id DESC
        LIMIT 20
        """
    )
    fun recentItemNames(bid: Long): Flow<List<String>>
}
