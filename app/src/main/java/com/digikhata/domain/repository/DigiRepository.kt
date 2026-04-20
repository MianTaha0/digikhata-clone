package com.digikhata.domain.repository

import com.digikhata.data.entity.Business
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.DigiNotification
import com.digikhata.data.entity.TxEntity
import kotlinx.coroutines.flow.Flow

interface DigiRepository {
    val businesses: Flow<List<Business>>
    suspend fun upsertBusiness(business: Business): Long
    suspend fun deleteBusiness(business: Business)
    fun getBusiness(id: Long): Flow<Business?>

    fun clients(businessId: Long, type: Int): Flow<List<Client>>
    fun searchClients(businessId: Long, type: Int, query: String): Flow<List<Client>>
    suspend fun upsertClient(client: Client): Long
    suspend fun deleteClient(client: Client)
    fun getClient(id: Long): Flow<Client?>

    fun transactions(clientId: Long): Flow<List<TxEntity>>
    fun balanceForClient(clientId: Long): Flow<Double>
    suspend fun addTransaction(tx: TxEntity, images: List<String>): Long
    suspend fun updateTransaction(tx: TxEntity)
    suspend fun deleteTransaction(tx: TxEntity)

    val notifications: Flow<List<DigiNotification>>
    suspend fun addNotification(notification: DigiNotification): Long
    suspend fun markNotificationSeen(id: Long)
}
