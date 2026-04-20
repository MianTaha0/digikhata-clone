package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_images",
    foreignKeys = [ForeignKey(
        entity = TxEntity::class,
        parentColumns = ["id"],
        childColumns = ["transactionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("transactionId")]
)
data class TransactionImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val localPath: String,
    val createdAt: Long = System.currentTimeMillis()
)
