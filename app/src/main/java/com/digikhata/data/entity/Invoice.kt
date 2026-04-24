package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Business::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("businessId"), Index("customerId")]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val customerId: Long,
    val sequenceNumber: Int,
    val issueDate: Long,
    val dueDate: Long? = null,
    val notes: String? = null,
    val discountValue: Double = 0.0,
    val discountIsPercent: Boolean = false,
    val amountPaid: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val serverUpdatedAt: Long? = null
)
