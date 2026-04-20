package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_entries",
    foreignKeys = [
        ForeignKey(
            entity = Business::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId")]
)
data class ExpenseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val note: String? = null,
    val entryDate: Long,
    val imageLocalPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
