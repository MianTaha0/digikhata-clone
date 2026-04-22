package com.digikhata.data.repository

import androidx.room.withTransaction
import com.digikhata.data.DigiDatabase
import com.digikhata.data.dao.BusinessDao
import com.digikhata.data.dao.CashEntryDao
import com.digikhata.data.dao.ClientDao
import com.digikhata.data.dao.ExpenseEntryDao
import com.digikhata.data.dao.InvoiceDao
import com.digikhata.data.dao.InvoiceItemDao
import com.digikhata.data.dao.NotificationDao
import com.digikhata.data.dao.ProductDao
import com.digikhata.data.dao.StaffDao
import com.digikhata.data.dao.StaffPaymentDao
import com.digikhata.data.dao.StockMovementDao
import com.digikhata.data.dao.TransactionDao
import com.digikhata.data.dao.TransactionImageDao
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.DigiNotification
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
import com.digikhata.data.entity.Product
import com.digikhata.data.entity.Staff
import com.digikhata.data.entity.StaffPayment
import com.digikhata.data.entity.StockMovement
import com.digikhata.data.entity.TransactionImage
import com.digikhata.data.entity.TxEntity
import com.digikhata.domain.model.CashTotals
import com.digikhata.domain.repository.DigiRepository
import com.digikhata.util.InvoiceCalc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val expenseEntryDao: ExpenseEntryDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val staffDao: StaffDao,
    private val staffPaymentDao: StaffPaymentDao,
    private val db: DigiDatabase
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

    override fun invoices(businessId: Long): Flow<List<Invoice>> =
        invoiceDao.getByBusiness(businessId)

    override fun getInvoice(id: Long): Flow<Invoice?> = invoiceDao.getById(id)

    override fun invoiceItems(invoiceId: Long): Flow<List<InvoiceItem>> =
        invoiceItemDao.getByInvoice(invoiceId)

    override fun recentItemNames(businessId: Long): Flow<List<String>> =
        invoiceDao.recentItemNames(businessId)

    override suspend fun nextInvoiceSequence(businessId: Long): Int =
        invoiceDao.nextSequenceNumber(businessId)

    override suspend fun saveInvoice(inv: Invoice, items: List<InvoiceItem>): Long {
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val id = if (inv.id == 0L) {
                val seq = invoiceDao.nextSequenceNumber(inv.businessId)
                val newInv = inv.copy(sequenceNumber = seq, createdAt = now, updatedAt = now)
                invoiceDao.insertInvoice(newInv)
            } else {
                invoiceDao.updateInvoice(inv.copy(updatedAt = now))
                invoiceItemDao.deleteByInvoice(inv.id)
                inv.id
            }
            invoiceItemDao.insertAll(
                items.mapIndexed { idx, it -> it.copy(id = 0, invoiceId = id, sortOrder = idx) }
            )
            id
        }
    }

    override suspend fun recordPayment(invoiceId: Long, amount: Double) {
        val inv = invoiceDao.getById(invoiceId).first() ?: return
        val items = invoiceItemDao.getByInvoice(invoiceId).first()
        val totals = InvoiceCalc.compute(inv, items)
        val newPaid = (inv.amountPaid + amount).coerceAtMost(totals.grandTotal)
        invoiceDao.updateInvoice(inv.copy(amountPaid = newPaid, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteInvoice(inv: Invoice) {
        invoiceDao.deleteInvoice(inv)
    }

    override fun products(businessId: Long): Flow<List<Product>> =
        productDao.getByBusiness(businessId)

    override fun getProduct(id: Long): Flow<Product?> = productDao.getById(id)

    override fun productMovements(productId: Long): Flow<List<StockMovement>> =
        stockMovementDao.getByProduct(productId)

    override fun inventoryItemCount(businessId: Long): Flow<Int> =
        productDao.itemCount(businessId)

    override fun inventoryTotalValue(businessId: Long): Flow<Double> =
        productDao.totalValue(businessId)

    override fun lowStockCount(businessId: Long): Flow<Int> =
        productDao.lowStockCount(businessId)

    override suspend fun addProduct(product: Product, imagePath: String?): Long {
        val now = System.currentTimeMillis()
        val prepared = product.copy(
            createdAt = if (product.createdAt == 0L) now else product.createdAt,
            updatedAt = if (product.updatedAt == 0L) now else product.updatedAt,
            imageLocalPath = imagePath ?: product.imageLocalPath
        )
        return productDao.insert(prepared)
    }

    override suspend fun updateProduct(product: Product) =
        productDao.update(product.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
        product.imageLocalPath?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    override fun staffList(businessId: Long): Flow<List<Staff>> =
        staffDao.getByBusiness(businessId)

    override fun getStaff(id: Long): Flow<Staff?> = staffDao.getById(id)

    override fun staffCount(businessId: Long): Flow<Int> = staffDao.staffCount(businessId)

    override fun totalMonthlyPayroll(businessId: Long): Flow<Double> =
        staffDao.totalMonthlyPayroll(businessId)

    override fun paidThisMonthForBusiness(businessId: Long, from: Long, to: Long): Flow<Double> =
        staffPaymentDao.paidThisMonthForBusiness(businessId, from, to)

    override fun paidByStaffInRange(businessId: Long, from: Long, to: Long): Flow<Map<Long, Double>> =
        staffPaymentDao.paidByStaffInRange(businessId, from, to).map { list ->
            list.associate { it.staffId to it.amount }
        }

    override suspend fun addStaff(staff: Staff, imagePath: String?): Long {
        val now = System.currentTimeMillis()
        val prepared = staff.copy(
            createdAt = if (staff.createdAt == 0L) now else staff.createdAt,
            updatedAt = if (staff.updatedAt == 0L) now else staff.updatedAt,
            imageLocalPath = imagePath ?: staff.imageLocalPath
        )
        return staffDao.insert(prepared)
    }

    override suspend fun updateStaff(staff: Staff) =
        staffDao.update(staff.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun deleteStaff(staff: Staff) {
        staffDao.delete(staff)
        staff.imageLocalPath?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    override fun staffPayments(staffId: Long): Flow<List<StaffPayment>> =
        staffPaymentDao.getByStaff(staffId)

    override fun paidInRange(staffId: Long, from: Long, to: Long): Flow<Double> =
        staffPaymentDao.paidBetween(staffId, from, to)

    override suspend fun addStaffPayment(payment: StaffPayment): Long {
        val now = System.currentTimeMillis()
        val prepared = if (payment.createdAt == 0L) payment.copy(createdAt = now) else payment
        return staffPaymentDao.insert(prepared)
    }

    override suspend fun deleteStaffPayment(payment: StaffPayment) {
        staffPaymentDao.delete(payment)
    }

    override suspend fun adjustStock(productId: Long, delta: Double, reason: String?) {
        db.withTransaction {
            val current = productDao.getById(productId).first() ?: return@withTransaction
            val now = System.currentTimeMillis()
            stockMovementDao.insert(
                StockMovement(
                    productId = productId,
                    delta = delta,
                    reason = reason,
                    createdAt = now
                )
            )
            productDao.update(
                current.copy(
                    quantity = current.quantity + delta,
                    updatedAt = now
                )
            )
        }
    }
}
