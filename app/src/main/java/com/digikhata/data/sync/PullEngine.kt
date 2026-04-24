package com.digikhata.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.digikhata.data.dao.BusinessDao
import com.digikhata.data.dao.CashEntryDao
import com.digikhata.data.dao.ClientDao
import com.digikhata.data.dao.ExpenseEntryDao
import com.digikhata.data.dao.InvoiceDao
import com.digikhata.data.dao.InvoiceItemDao
import com.digikhata.data.dao.ProductDao
import com.digikhata.data.dao.StaffAttendanceDao
import com.digikhata.data.dao.StaffDao
import com.digikhata.data.dao.StaffPaymentDao
import com.digikhata.data.dao.StockMovementDao
import com.digikhata.data.dao.TransactionDao
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
import com.digikhata.data.entity.Product
import com.digikhata.data.entity.Staff
import com.digikhata.data.entity.StaffAttendance
import com.digikhata.data.entity.StaffPayment
import com.digikhata.data.entity.StockMovement
import com.digikhata.data.entity.TxEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 3b.3: walks every syncable collection under `users/{uid}/businesses/...`
 * and merges remote docs into Room using last-writer-wins.
 *
 * Does NOT enqueue `SyncOp`s — writes bypass the repository's cloudSync hook by
 * calling DAOs directly.
 */
interface PullEngine {
    suspend fun pullOnce(uid: String): Result<PullSummary>
}

data class PullSummary(
    val docsApplied: Int,
    val conflictsResolved: Int
)

@Singleton
class PullEngineImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val firestoreProvider: FirestoreProvider,
    private val businessDao: BusinessDao,
    private val clientDao: ClientDao,
    private val transactionDao: TransactionDao,
    private val cashEntryDao: CashEntryDao,
    private val expenseEntryDao: ExpenseEntryDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val staffDao: StaffDao,
    private val staffPaymentDao: StaffPaymentDao,
    private val staffAttendanceDao: StaffAttendanceDao
) : PullEngine {

    private val prefs: SharedPreferences by lazy {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun pullOnce(uid: String): Result<PullSummary> = runCatching {
        val firestore = firestoreProvider.get()

        var applied = 0
        var conflicts = 0

        // Pull top-level business docs (users/{uid}/businesses/{id}).
        val businessesRef = firestore.collection("users").document(uid).collection("businesses")
        val businessKey = collectionKey(uid, null, "businesses")
        val businessesResult = pullCollection(businessesRef, businessKey) { doc ->
            applyBusiness(doc)
        }
        applied += businessesResult.applied
        conflicts += businessesResult.conflicts

        // For each local business, pull each child collection.
        val businesses = businessDao.getAll().first()
        for (biz in businesses) {
            for (coll in CHILD_COLLECTIONS) {
                val key = collectionKey(uid, biz.id, coll)
                val ref = firestore.collection("users").document(uid)
                    .collection("businesses").document(biz.id.toString())
                    .collection(coll)
                val r = pullCollection(ref, key) { doc -> applyDoc(coll, doc) }
                applied += r.applied
                conflicts += r.conflicts
            }
        }

        prefs.edit().putLong(KEY_LAST_PULL_AT, System.currentTimeMillis()).apply()
        PullSummary(applied, conflicts)
    }

    private data class CollectionResult(val applied: Int, val conflicts: Int)

    private suspend fun pullCollection(
        ref: com.google.firebase.firestore.CollectionReference,
        prefKey: String,
        apply: suspend (DocumentSnapshot) -> ApplyOutcome
    ): CollectionResult {
        val since = prefs.getLong(prefKey, 0L)
        val snap = ref
            .whereGreaterThan("updatedAt", since)
            .orderBy("updatedAt", Query.Direction.ASCENDING)
            .get()
            .await()
        var applied = 0
        var conflicts = 0
        var maxSeen = since
        for (doc in snap.documents) {
            val out = apply(doc)
            if (out.applied) applied++
            if (out.conflict) conflicts++
            if (out.remoteUpdatedAt > maxSeen) maxSeen = out.remoteUpdatedAt
        }
        if (maxSeen > since) prefs.edit().putLong(prefKey, maxSeen).apply()
        return CollectionResult(applied, conflicts)
    }

    private data class ApplyOutcome(
        val applied: Boolean,
        val conflict: Boolean,
        val remoteUpdatedAt: Long
    )

    // --- per-collection appliers ---

    private suspend fun applyBusiness(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        val id = data.asLong("id") ?: return ApplyOutcome(false, false, remote.updatedAt)
        val local = businessDao.getById(id).first()
        return when (val d = decideMerge(local?.let { LocalSnapshot(it.updatedAt, it.deletedAt) }, remote)) {
            MergeDecision.SkipLocalNewer -> ApplyOutcome(false, true, remote.updatedAt)
            is MergeDecision.InsertNew,
            is MergeDecision.ApplyRemote,
            is MergeDecision.ApplyRemoteDelete -> {
                val entity = mapToBusiness(data, remote.updatedAt)
                businessDao.insert(entity)
                ApplyOutcome(true, d is MergeDecision.ApplyRemoteDelete, remote.updatedAt)
            }
        }
    }

    private suspend fun applyDoc(collection: String, doc: DocumentSnapshot): ApplyOutcome =
        when (collection) {
            "clients" -> applyClient(doc)
            "transactions" -> applyTransaction(doc)
            "cashEntries" -> applyCashEntry(doc)
            "expenseEntries" -> applyExpense(doc)
            "invoices" -> applyInvoice(doc)
            "invoiceItems" -> applyInvoiceItem(doc)
            "products" -> applyProduct(doc)
            "stockMovements" -> applyStockMovement(doc)
            "staff" -> applyStaff(doc)
            "staffPayments" -> applyStaffPayment(doc)
            "staffAttendance" -> applyStaffAttendance(doc)
            else -> ApplyOutcome(false, false, 0L)
        }

    private suspend fun applyClient(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        val id = data.asLong("id") ?: return ApplyOutcome(false, false, remote.updatedAt)
        val local = clientDao.getById(id).first()
        val decision = decideMerge(local?.let { LocalSnapshot(it.updatedAt, it.deletedAt) }, remote)
        if (decision is MergeDecision.SkipLocalNewer) return ApplyOutcome(false, true, remote.updatedAt)
        clientDao.insert(mapToClient(data, remote.updatedAt))
        return ApplyOutcome(true, decision is MergeDecision.ApplyRemoteDelete, remote.updatedAt)
    }

    private suspend fun applyTransaction(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        val id = data.asLong("id") ?: return ApplyOutcome(false, false, remote.updatedAt)
        val local = transactionDao.getById(id).first()
        val decision = decideMerge(local?.let { LocalSnapshot(it.updatedAt, it.deletedAt) }, remote)
        if (decision is MergeDecision.SkipLocalNewer) return ApplyOutcome(false, true, remote.updatedAt)
        transactionDao.insert(mapToTransaction(data, remote.updatedAt))
        return ApplyOutcome(true, decision is MergeDecision.ApplyRemoteDelete, remote.updatedAt)
    }

    private suspend fun applyCashEntry(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        val id = data.asLong("id") ?: return ApplyOutcome(false, false, remote.updatedAt)
        val local = cashEntryDao.getById(id).first()
        val decision = decideMerge(local?.let { LocalSnapshot(it.updatedAt, it.deletedAt) }, remote)
        if (decision is MergeDecision.SkipLocalNewer) return ApplyOutcome(false, true, remote.updatedAt)
        cashEntryDao.insert(mapToCashEntry(data, remote.updatedAt))
        return ApplyOutcome(true, decision is MergeDecision.ApplyRemoteDelete, remote.updatedAt)
    }

    private suspend fun applyExpense(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        val id = data.asLong("id") ?: return ApplyOutcome(false, false, remote.updatedAt)
        val local = expenseEntryDao.getById(id).first()
        val decision = decideMerge(local?.let { LocalSnapshot(it.updatedAt, it.deletedAt) }, remote)
        if (decision is MergeDecision.SkipLocalNewer) return ApplyOutcome(false, true, remote.updatedAt)
        expenseEntryDao.insert(mapToExpense(data, remote.updatedAt))
        return ApplyOutcome(true, decision is MergeDecision.ApplyRemoteDelete, remote.updatedAt)
    }

    private suspend fun applyInvoice(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        val id = data.asLong("id") ?: return ApplyOutcome(false, false, remote.updatedAt)
        val local = invoiceDao.getById(id).first()
        val decision = decideMerge(local?.let { LocalSnapshot(it.updatedAt, it.deletedAt) }, remote)
        if (decision is MergeDecision.SkipLocalNewer) return ApplyOutcome(false, true, remote.updatedAt)
        invoiceDao.insertInvoice(mapToInvoice(data, remote.updatedAt))
        return ApplyOutcome(true, decision is MergeDecision.ApplyRemoteDelete, remote.updatedAt)
    }

    private suspend fun applyInvoiceItem(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        invoiceItemDao.insertAll(listOf(mapToInvoiceItem(data, remote.updatedAt)))
        return ApplyOutcome(true, remote.deletedAt != null, remote.updatedAt)
    }

    private suspend fun applyProduct(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        val id = data.asLong("id") ?: return ApplyOutcome(false, false, remote.updatedAt)
        val local = productDao.getById(id).first()
        val decision = decideMerge(local?.let { LocalSnapshot(it.updatedAt, it.deletedAt) }, remote)
        if (decision is MergeDecision.SkipLocalNewer) return ApplyOutcome(false, true, remote.updatedAt)
        productDao.insert(mapToProduct(data, remote.updatedAt))
        return ApplyOutcome(true, decision is MergeDecision.ApplyRemoteDelete, remote.updatedAt)
    }

    private suspend fun applyStockMovement(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        stockMovementDao.insert(mapToStockMovement(data, remote.updatedAt))
        return ApplyOutcome(true, remote.deletedAt != null, remote.updatedAt)
    }

    private suspend fun applyStaff(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        val id = data.asLong("id") ?: return ApplyOutcome(false, false, remote.updatedAt)
        val local = staffDao.getById(id).first()
        val decision = decideMerge(local?.let { LocalSnapshot(it.updatedAt, it.deletedAt) }, remote)
        if (decision is MergeDecision.SkipLocalNewer) return ApplyOutcome(false, true, remote.updatedAt)
        staffDao.insert(mapToStaff(data, remote.updatedAt))
        return ApplyOutcome(true, decision is MergeDecision.ApplyRemoteDelete, remote.updatedAt)
    }

    private suspend fun applyStaffPayment(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        staffPaymentDao.insert(mapToStaffPayment(data, remote.updatedAt))
        return ApplyOutcome(true, remote.deletedAt != null, remote.updatedAt)
    }

    private suspend fun applyStaffAttendance(doc: DocumentSnapshot): ApplyOutcome {
        val data = doc.data ?: return ApplyOutcome(false, false, 0L)
        val remote = remoteOf(data) ?: return ApplyOutcome(false, false, 0L)
        staffAttendanceDao.upsert(mapToStaffAttendance(data, remote.updatedAt))
        return ApplyOutcome(true, remote.deletedAt != null, remote.updatedAt)
    }

    // --- mappers: Firestore doc Map → Room entity ---

    private fun mapToBusiness(d: Map<String, Any?>, serverUpdated: Long): Business = Business(
        id = d.asLong("id") ?: 0L,
        name = d.asString("name") ?: "",
        ownerName = d.asString("ownerName"),
        phone = d.asString("phone"),
        currency = d.asString("currency") ?: "Pakistan Rupee-Rs",
        colorHex = d.asString("colorHex") ?: "#E74425",
        address = d.asString("address"),
        city = d.asString("city"),
        type = d.asString("type"),
        category = d.asString("category"),
        tagline = d.asString("tagline"),
        logoLocalPath = d.asString("logoLocalPath"),
        invoicePrefix = d.asString("invoicePrefix") ?: "INV-",
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToClient(d: Map<String, Any?>, serverUpdated: Long): Client = Client(
        id = d.asLong("id") ?: 0L,
        businessId = d.asLong("businessId") ?: 0L,
        type = d.asInt("type") ?: 0,
        name = d.asString("name") ?: "",
        phone = d.asString("phone"),
        phone2 = d.asString("phone2"),
        cnic = d.asString("cnic"),
        address = d.asString("address"),
        creditLimit = d.asDouble("creditLimit") ?: 0.0,
        rating = d.asInt("rating") ?: 0,
        isPinned = d.asBoolean("isPinned") ?: false,
        isArchived = d.asBoolean("isArchived") ?: false,
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToTransaction(d: Map<String, Any?>, serverUpdated: Long): TxEntity = TxEntity(
        id = d.asLong("id") ?: 0L,
        clientId = d.asLong("clientId") ?: 0L,
        businessId = d.asLong("businessId") ?: 0L,
        amount = d.asDouble("amount") ?: 0.0,
        type = d.asInt("type") ?: 0,
        notes = d.asString("notes"),
        entryDate = d.asLong("entryDate") ?: 0L,
        imageLocalPath = d.asString("imageLocalPath"),
        imagesCount = d.asInt("imagesCount") ?: 0,
        smsStatus = d.asInt("smsStatus") ?: -1,
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToCashEntry(d: Map<String, Any?>, serverUpdated: Long): CashEntry = CashEntry(
        id = d.asLong("id") ?: 0L,
        businessId = d.asLong("businessId") ?: 0L,
        amount = d.asDouble("amount") ?: 0.0,
        type = d.asInt("type") ?: 0,
        category = d.asString("category") ?: "",
        note = d.asString("note"),
        entryDate = d.asLong("entryDate") ?: 0L,
        imageLocalPath = d.asString("imageLocalPath"),
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToExpense(d: Map<String, Any?>, serverUpdated: Long): ExpenseEntry = ExpenseEntry(
        id = d.asLong("id") ?: 0L,
        businessId = d.asLong("businessId") ?: 0L,
        amount = d.asDouble("amount") ?: 0.0,
        category = d.asString("category") ?: "",
        paymentMethod = d.asString("paymentMethod") ?: "",
        note = d.asString("note"),
        entryDate = d.asLong("entryDate") ?: 0L,
        imageLocalPath = d.asString("imageLocalPath"),
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToInvoice(d: Map<String, Any?>, serverUpdated: Long): Invoice = Invoice(
        id = d.asLong("id") ?: 0L,
        businessId = d.asLong("businessId") ?: 0L,
        customerId = d.asLong("customerId") ?: 0L,
        sequenceNumber = d.asInt("sequenceNumber") ?: 0,
        issueDate = d.asLong("issueDate") ?: 0L,
        dueDate = d.asLong("dueDate"),
        notes = d.asString("notes"),
        discountValue = d.asDouble("discountValue") ?: 0.0,
        discountIsPercent = d.asBoolean("discountIsPercent") ?: false,
        amountPaid = d.asDouble("amountPaid") ?: 0.0,
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToInvoiceItem(d: Map<String, Any?>, serverUpdated: Long): InvoiceItem = InvoiceItem(
        id = d.asLong("id") ?: 0L,
        invoiceId = d.asLong("invoiceId") ?: 0L,
        name = d.asString("name") ?: "",
        quantity = d.asDouble("quantity") ?: 0.0,
        unitPrice = d.asDouble("unitPrice") ?: 0.0,
        taxPercent = d.asDouble("taxPercent") ?: 0.0,
        sortOrder = d.asInt("sortOrder") ?: 0,
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToProduct(d: Map<String, Any?>, serverUpdated: Long): Product = Product(
        id = d.asLong("id") ?: 0L,
        businessId = d.asLong("businessId") ?: 0L,
        name = d.asString("name") ?: "",
        sku = d.asString("sku"),
        costPrice = d.asDouble("costPrice") ?: 0.0,
        sellPrice = d.asDouble("sellPrice") ?: 0.0,
        quantity = d.asDouble("quantity") ?: 0.0,
        lowStockThreshold = d.asDouble("lowStockThreshold") ?: 0.0,
        unit = d.asString("unit") ?: "pcs",
        imageLocalPath = d.asString("imageLocalPath"),
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToStockMovement(d: Map<String, Any?>, serverUpdated: Long): StockMovement = StockMovement(
        id = d.asLong("id") ?: 0L,
        productId = d.asLong("productId") ?: 0L,
        delta = d.asDouble("delta") ?: 0.0,
        reason = d.asString("reason"),
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToStaff(d: Map<String, Any?>, serverUpdated: Long): Staff = Staff(
        id = d.asLong("id") ?: 0L,
        businessId = d.asLong("businessId") ?: 0L,
        name = d.asString("name") ?: "",
        role = d.asString("role"),
        phone = d.asString("phone"),
        monthlySalary = d.asDouble("monthlySalary") ?: 0.0,
        joiningDate = d.asLong("joiningDate") ?: 0L,
        imageLocalPath = d.asString("imageLocalPath"),
        notes = d.asString("notes"),
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToStaffPayment(d: Map<String, Any?>, serverUpdated: Long): StaffPayment = StaffPayment(
        id = d.asLong("id") ?: 0L,
        staffId = d.asLong("staffId") ?: 0L,
        amount = d.asDouble("amount") ?: 0.0,
        paymentDate = d.asLong("paymentDate") ?: 0L,
        note = d.asString("note"),
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun mapToStaffAttendance(d: Map<String, Any?>, serverUpdated: Long): StaffAttendance = StaffAttendance(
        id = d.asLong("id") ?: 0L,
        staffId = d.asLong("staffId") ?: 0L,
        date = d.asLong("date") ?: 0L,
        status = d.asString("status") ?: "",
        notes = d.asString("notes"),
        createdAt = d.asLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = d.asLong("updatedAt") ?: System.currentTimeMillis(),
        deletedAt = d.asLong("deletedAt"),
        serverUpdatedAt = serverUpdated
    )

    private fun remoteOf(d: Map<String, Any?>): RemoteSnapshot? {
        val updated = d.asLong("updatedAt") ?: return null
        return RemoteSnapshot(
            updatedAt = updated,
            deletedAt = d.asLong("deletedAt"),
            data = d
        )
    }

    private fun collectionKey(uid: String, businessId: Long?, collection: String): String =
        "$KEY_PULL_PREFIX:$uid:${businessId ?: "_"}:$collection"

    companion object {
        private const val PREFS_NAME = "digikhata_sync_prefs"
        private const val KEY_PULL_PREFIX = "pull_since"
        const val KEY_LAST_PULL_AT = "last_pull_at"

        private val CHILD_COLLECTIONS = listOf(
            "clients",
            "transactions",
            "cashEntries",
            "expenseEntries",
            "invoices",
            "invoiceItems",
            "products",
            "stockMovements",
            "staff",
            "staffPayments",
            "staffAttendance"
        )
    }
}

// Convenience accessors that tolerate Firestore's Number/Long/Double mixing.
private fun Map<String, Any?>.asLong(key: String): Long? = (this[key] as? Number)?.toLong()
private fun Map<String, Any?>.asInt(key: String): Int? = (this[key] as? Number)?.toInt()
private fun Map<String, Any?>.asDouble(key: String): Double? = (this[key] as? Number)?.toDouble()
private fun Map<String, Any?>.asString(key: String): String? = this[key] as? String
private fun Map<String, Any?>.asBoolean(key: String): Boolean? = this[key] as? Boolean
