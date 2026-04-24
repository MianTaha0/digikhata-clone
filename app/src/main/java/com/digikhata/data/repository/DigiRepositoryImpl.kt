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
import com.digikhata.data.dao.StaffAttendanceDao
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
import com.digikhata.data.entity.StaffAttendance
import com.digikhata.data.entity.StaffPayment
import com.digikhata.data.entity.StockMovement
import com.digikhata.data.entity.TransactionImage
import com.digikhata.data.entity.TxEntity
import com.digikhata.data.sync.CloudSyncRepository
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
    private val staffAttendanceDao: StaffAttendanceDao,
    private val db: DigiDatabase,
    private val cloudSync: CloudSyncRepository
) : DigiRepository {

    override val businesses: Flow<List<Business>> = businessDao.getAll()

    override suspend fun upsertBusiness(business: Business): Long {
        val id = if (business.id == 0L) {
            businessDao.insert(business)
        } else {
            businessDao.update(business.copy(updatedAt = System.currentTimeMillis()))
            business.id
        }
        val final = if (business.id == 0L) business.copy(id = id) else business
        // Businesses live at users/{uid}/businesses/{id}, so businessId parent is null.
        cloudSync.onUpsert(null, "businesses", id.toString(), final)
        return id
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete + tombstones.
    override suspend fun deleteBusiness(business: Business) {
        businessDao.delete(business)
        cloudSync.onDelete(null, "businesses", business.id.toString())
    }
    override fun getBusiness(id: Long): Flow<Business?> = businessDao.getById(id)

    override fun clients(businessId: Long, type: Int): Flow<List<Client>> =
        clientDao.getByBusinessAndType(businessId, type)

    override fun searchClients(businessId: Long, type: Int, query: String): Flow<List<Client>> =
        clientDao.search(businessId, type, query)

    override suspend fun upsertClient(client: Client): Long {
        val id = if (client.id == 0L) {
            clientDao.insert(client)
        } else {
            clientDao.update(client.copy(updatedAt = System.currentTimeMillis()))
            client.id
        }
        val final = if (client.id == 0L) client.copy(id = id) else client
        cloudSync.onUpsert(final.businessId, "clients", id.toString(), final)
        return id
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun deleteClient(client: Client) {
        clientDao.delete(client)
        cloudSync.onDelete(client.businessId, "clients", client.id.toString())
    }
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
        val finalTx = if (images.isNotEmpty()) {
            val updated = tx.copy(
                id = txId,
                imagesCount = images.size,
                imageLocalPath = images.first(),
                updatedAt = System.currentTimeMillis()
            )
            transactionDao.update(updated)
            updated
        } else {
            tx.copy(id = txId)
        }
        cloudSync.onUpsert(finalTx.businessId, "transactions", txId.toString(), finalTx)
        return txId
    }

    override suspend fun updateTransaction(tx: TxEntity) {
        val updated = tx.copy(updatedAt = System.currentTimeMillis())
        transactionDao.update(updated)
        cloudSync.onUpsert(updated.businessId, "transactions", updated.id.toString(), updated)
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun deleteTransaction(tx: TxEntity) {
        transactionDao.delete(tx)
        cloudSync.onDelete(tx.businessId, "transactions", tx.id.toString())
    }

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
        val id = cashEntryDao.insert(prepared)
        val final = prepared.copy(id = id)
        cloudSync.onUpsert(final.businessId, "cashEntries", id.toString(), final)
        return id
    }

    override suspend fun updateCashEntry(entry: CashEntry) {
        val updated = entry.copy(updatedAt = System.currentTimeMillis())
        cashEntryDao.update(updated)
        cloudSync.onUpsert(updated.businessId, "cashEntries", updated.id.toString(), updated)
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun deleteCashEntry(entry: CashEntry) {
        cashEntryDao.delete(entry)
        entry.imageLocalPath?.let { path ->
            runCatching { File(path).delete() }
        }
        cloudSync.onDelete(entry.businessId, "cashEntries", entry.id.toString())
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
        val id = expenseEntryDao.insert(prepared)
        val final = prepared.copy(id = id)
        cloudSync.onUpsert(final.businessId, "expenseEntries", id.toString(), final)
        return id
    }

    override suspend fun updateExpense(entry: ExpenseEntry) {
        val updated = entry.copy(updatedAt = System.currentTimeMillis())
        expenseEntryDao.update(updated)
        cloudSync.onUpsert(updated.businessId, "expenseEntries", updated.id.toString(), updated)
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun deleteExpense(entry: ExpenseEntry) {
        expenseEntryDao.delete(entry)
        entry.imageLocalPath?.let { path ->
            runCatching { File(path).delete() }
        }
        cloudSync.onDelete(entry.businessId, "expenseEntries", entry.id.toString())
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
        data class Saved(val invoice: Invoice, val items: List<InvoiceItem>)
        val saved = db.withTransaction {
            val now = System.currentTimeMillis()
            val (id, finalInv) = if (inv.id == 0L) {
                val seq = invoiceDao.nextSequenceNumber(inv.businessId)
                val newInv = inv.copy(sequenceNumber = seq, createdAt = now, updatedAt = now)
                val newId = invoiceDao.insertInvoice(newInv)
                newId to newInv.copy(id = newId)
            } else {
                val updated = inv.copy(updatedAt = now)
                invoiceDao.updateInvoice(updated)
                invoiceItemDao.deleteByInvoice(inv.id)
                inv.id to updated
            }
            val stampedItems = items.mapIndexed { idx, it ->
                it.copy(id = 0, invoiceId = id, sortOrder = idx)
            }
            invoiceItemDao.insertAll(stampedItems)
            // Re-read items so we have the DB-assigned ids for sync.
            val reloaded = invoiceItemDao.getByInvoice(id).first()
            Saved(finalInv, reloaded)
        }
        cloudSync.onUpsert(saved.invoice.businessId, "invoices", saved.invoice.id.toString(), saved.invoice)
        saved.items.forEach { item ->
            cloudSync.onUpsert(saved.invoice.businessId, "invoiceItems", item.id.toString(), item)
        }
        return saved.invoice.id
    }

    override suspend fun recordPayment(invoiceId: Long, amount: Double) {
        val inv = invoiceDao.getById(invoiceId).first() ?: return
        val items = invoiceItemDao.getByInvoice(invoiceId).first()
        val totals = InvoiceCalc.compute(inv, items)
        val newPaid = (inv.amountPaid + amount).coerceAtMost(totals.grandTotal)
        val updated = inv.copy(amountPaid = newPaid, updatedAt = System.currentTimeMillis())
        invoiceDao.updateInvoice(updated)
        cloudSync.onUpsert(updated.businessId, "invoices", updated.id.toString(), updated)
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun deleteInvoice(inv: Invoice) {
        invoiceDao.deleteInvoice(inv)
        cloudSync.onDelete(inv.businessId, "invoices", inv.id.toString())
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
        val id = productDao.insert(prepared)
        val final = prepared.copy(id = id)
        cloudSync.onUpsert(final.businessId, "products", id.toString(), final)
        return id
    }

    override suspend fun updateProduct(product: Product) {
        val updated = product.copy(updatedAt = System.currentTimeMillis())
        productDao.update(updated)
        cloudSync.onUpsert(updated.businessId, "products", updated.id.toString(), updated)
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
        product.imageLocalPath?.let { path ->
            runCatching { File(path).delete() }
        }
        cloudSync.onDelete(product.businessId, "products", product.id.toString())
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
        val id = staffDao.insert(prepared)
        val final = prepared.copy(id = id)
        cloudSync.onUpsert(final.businessId, "staff", id.toString(), final)
        return id
    }

    override suspend fun updateStaff(staff: Staff) {
        val updated = staff.copy(updatedAt = System.currentTimeMillis())
        staffDao.update(updated)
        cloudSync.onUpsert(updated.businessId, "staff", updated.id.toString(), updated)
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun deleteStaff(staff: Staff) {
        staffDao.delete(staff)
        staff.imageLocalPath?.let { path ->
            runCatching { File(path).delete() }
        }
        cloudSync.onDelete(staff.businessId, "staff", staff.id.toString())
    }

    override fun staffPayments(staffId: Long): Flow<List<StaffPayment>> =
        staffPaymentDao.getByStaff(staffId)

    override fun paidInRange(staffId: Long, from: Long, to: Long): Flow<Double> =
        staffPaymentDao.paidBetween(staffId, from, to)

    override suspend fun addStaffPayment(payment: StaffPayment): Long {
        val now = System.currentTimeMillis()
        val prepared = if (payment.createdAt == 0L) payment.copy(createdAt = now) else payment
        val id = staffPaymentDao.insert(prepared)
        val final = prepared.copy(id = id)
        val businessId = staffDao.getById(payment.staffId).first()?.businessId
        cloudSync.onUpsert(businessId, "staffPayments", id.toString(), final)
        return id
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun deleteStaffPayment(payment: StaffPayment) {
        val businessId = staffDao.getById(payment.staffId).first()?.businessId
        staffPaymentDao.delete(payment)
        cloudSync.onDelete(businessId, "staffPayments", payment.id.toString())
    }

    override fun observeAttendance(staffId: Long, from: Long, to: Long): Flow<List<StaffAttendance>> =
        staffAttendanceDao.observeRange(staffId, from, to)

    override suspend fun upsertAttendance(record: StaffAttendance): Long {
        val now = System.currentTimeMillis()
        val prepared = record.copy(
            createdAt = if (record.createdAt == 0L) now else record.createdAt,
            updatedAt = now
        )
        val id = staffAttendanceDao.upsert(prepared)
        val final = if (prepared.id == 0L) prepared.copy(id = id) else prepared
        val businessId = staffDao.getById(record.staffId).first()?.businessId
        cloudSync.onUpsert(businessId, "staffAttendance", id.toString(), final)
        return id
    }

    // Hard delete for now; Phase 3b.3 will switch to soft-delete.
    override suspend fun clearAttendance(staffId: Long, date: Long) {
        staffAttendanceDao.clear(staffId, date)
        val businessId = staffDao.getById(staffId).first()?.businessId
        // We don't have the row id here; use a compound key so 3b.3 can reconcile.
        cloudSync.onDelete(businessId, "staffAttendance", "$staffId-$date")
    }

    override suspend fun adjustStock(productId: Long, delta: Double, reason: String?) {
        data class Adjusted(val product: Product, val movement: StockMovement)
        val result: Adjusted? = db.withTransaction {
            val current = productDao.getById(productId).first() ?: return@withTransaction null
            val now = System.currentTimeMillis()
            val movement = StockMovement(
                productId = productId,
                delta = delta,
                reason = reason,
                createdAt = now
            )
            val movementId = stockMovementDao.insert(movement)
            val updated = current.copy(
                quantity = current.quantity + delta,
                updatedAt = now
            )
            productDao.update(updated)
            Adjusted(updated, movement.copy(id = movementId))
        }
        if (result != null) {
            cloudSync.onUpsert(
                result.product.businessId,
                "products",
                result.product.id.toString(),
                result.product
            )
            cloudSync.onUpsert(
                result.product.businessId,
                "stockMovements",
                result.movement.id.toString(),
                result.movement
            )
        }
    }
}
