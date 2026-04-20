package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cash_entries",
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
data class CashEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val amount: Double,
    val type: Int, // 0 = Cash Out, 1 = Cash In
    val category: String,
    val note: String? = null,
    val entryDate: Long,
    val imageLocalPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
