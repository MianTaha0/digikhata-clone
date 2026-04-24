package com.digikhata.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 3b.2: pending cloud-sync operation queue row.
 * Each local Room write for a business-owned entity enqueues one of these.
 */
@Entity(tableName = "sync_ops", indices = [Index("createdAt")])
data class SyncOp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long?,          // null for Business ops themselves
    val collection: String,          // "clients", "invoices", etc
    val docId: String,               // room row id as string
    val opType: String,              // "UPSERT" | "DELETE"
    val payloadJson: String,         // serialized entity, empty string if DELETE
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null
)
