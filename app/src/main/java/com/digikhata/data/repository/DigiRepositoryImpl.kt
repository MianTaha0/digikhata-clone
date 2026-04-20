package com.digikhata.data.repository

import com.digikhata.data.dao.BusinessDao
import com.digikhata.data.dao.CashEntryDao
import com.digikhata.data.dao.ClientDao
import com.digikhata.data.dao.ExpenseEntryDao
import com.digikhata.data.dao.NotificationDao
import com.digikhata.data.dao.TransactionDao
import com.digikhata.data.dao.TransactionImageDao
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.DigiNotification
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.TransactionImage
import com.digikhata.data.entity.TxEntity
import com.digikhata.domain.model.CashTotals
import com.digikhata.domain.repository.DigiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DigiRepositoryImpl @Inject constructor(
    private val businessDao: BusinessDao,
    private val clientDao: ClientDao,
    private val transactionDao: TransactionDao,
    private val transactionImageDao: TransactionImageDao,
    private val notificationDao: NotificationDao,
    private val cashEntryDao: CashEntryDao,
    private val expenseEntryDao: ExpenseEntryDao
) : DigiRepository {

    override val businesses: Flow<List<Business>> = businessDao.getAll()

    override suspend fun upsertBusiness(business: Business): Long {
        return if (business.id == 0L) {
            businessDao.insert(business)
        } else {
            businessDao.update(business.copy(updatedAt = System.currentTimeMillis()))
            business.id
        }
    }

    override suspend fun deleteBusiness(business: Business) = businessDao.delete(business)
    override fun getBusiness(id: Long): Flow<Business?> = businessDao.getById(id)

    override fun clients(businessId: Long, type: Int): Flow<List<Client>> =
        clientDao.getByBusinessAndType(businessId, type)

    override fun searchClients(businessId: Long, type: Int, query: String): Flow<List<Client>> =
        clientDao.search(businessId, type, query)

    override suspend fun upsertClient(client: Client): Long {
        return if (client.id == 0L) {
            clientDao.insert(client)
        } else {
            clientDao.update(client.copy(updatedAt = System.currentTimeMillis()))
            client.id
        }
    }

    override suspend fun deleteClient(client: Client) = clientDao.delete(client)
    override fun getClient(id: Long): Flow<Client?> = clientDao.getById(id)

    override fun transactions(clientId: Long): Flow<List<TxEntity>> =
        transactionDao.getByClient(clientId)

    override fun balanceForClient(clientId: Long): Flow<Double> =
        transactionDao.balanceForClient(clientId).map { it ?: 0.0 }

    override suspend fun addTransaction(tx: TxEntity, images: List<String>): Long {
        val txId = transactionDao.insert(tx)
        images.forEach { path ->
            transactionImageDao.insert(TransactionImage(transactionId = txId, localPath = path))
        }
        if (images.isNotEmpty()) {
            transactionDao.update(
                tx.copy(
                    id = txId,
                    imagesCount = images.size,
                    imageLocalPath = images.first(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return txId
    }

    override suspend fun updateTransaction(tx: TxEntity) =
        transactionDao.update(tx.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun deleteTransaction(tx: TxEntity) = transactionDao.delete(tx)

    override val notifications: Flow<List<DigiNotification>> = notificationDao.getAll()
    override suspend fun addNotification(notification: DigiNotification): Long =
        notificationDao.insert(notification)

    override suspend fun markNotificationSeen(id: Long) = notificationDao.markSeen(id)

    override fun cashEntries(businessId: Long, from: Long, to: Long): Flow<List<CashEntry>> =
        cashEntryDao.getInRange(businessId, from, to)

    override fun cashTotals(businessId: Long, from: Long, to: Long): Flow<CashTotals> =
        cashEntryDao.totalsForPeriod(businessId, from, to)

    override fun getCashEntry(id: Long): Flow<CashEntry?> = cashEntryDao.getById(id)

    override suspend fun addCashEntry(entry: CashEntry, imagePath: String?): Long {
        val now = System.currentTimeMillis()
        val prepared = entry.copy(
            createdAt = if (entry.createdAt == 0L) now else entry.createdAt,
            updatedAt = if (entry.updatedAt == 0L) now else entry.updatedAt,
            imageLocalPath = imagePath ?: entry.imageLocalPath
        )
        return cashEntryDao.insert(prepared)
    }

    override suspend fun updateCashEntry(entry: CashEntry) =
        cashEntryDao.update(entry.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun deleteCashEntry(entry: CashEntry) {
        cashEntryDao.delete(entry)
        entry.imageLocalPath?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    override fun expenses(businessId: Long, from: Long, to: Long): Flow<List<ExpenseEntry>> =
        expenseEntryDao.getInRange(businessId, from, to)

    override fun expenseTotal(businessId: Long, from: Long, to: Long): Flow<Double> =
        expenseEntryDao.totalForPeriod(businessId, from, to)

    override fun getExpense(id: Long): Flow<ExpenseEntry?> = expenseEntryDao.getById(id)

    override suspend fun addExpense(entry: ExpenseEntry, imagePath: String?): Long {
        val now = System.currentTimeMillis()
        val prepared = entry.copy(
            createdAt = if (entry.createdAt == 0L) now else entry.createdAt,
            updatedAt = if (entry.updatedAt == 0L) now else entry.updatedAt,
            imageLocalPath = imagePath ?: entry.imageLocalPath
        )
        return expenseEntryDao.insert(prepared)
    }

    override suspend fun updateExpense(entry: ExpenseEntry) =
        expenseEntryDao.update(entry.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun deleteExpense(entry: ExpenseEntry) {
        expenseEntryDao.delete(entry)
        entry.imageLocalPath?.let { path ->
            runCatching { File(path).delete() }
        }
    }
}
