package com.digikhata.ui.invoice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.Invoice
import com.digikhata.domain.model.InvoiceStatus
import com.digikhata.domain.model.InvoiceTotals
import com.digikhata.domain.repository.DigiRepository
import com.digikhata.util.InvoiceCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CreateEditInvoiceViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val invoiceId: Long = savedStateHandle.get<Long>("invoiceId") ?: 0L
    val isEdit: Boolean = invoiceId != 0L

    val customer: MutableStateFlow<Client?> = MutableStateFlow(null)
    val issueDate: MutableStateFlow<Long> = MutableStateFlow(System.currentTimeMillis())
    val dueDate: MutableStateFlow<Long?> = MutableStateFlow(null)
    val notes: MutableStateFlow<String> = MutableStateFlow("")
    val discountValueStr: MutableStateFlow<String> = MutableStateFlow("0")
    val discountIsPercent: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val items: MutableStateFlow<List<DraftItem>> = MutableStateFlow(listOf(DraftItem()))

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    val prefix: StateFlow<String> = active.active
        .map { it?.invoicePrefix ?: "INV-" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "INV-")

    val activeBookId: StateFlow<Long?> = active.id
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentItemNames: StateFlow<List<String>> = active.id.flatMapLatest { bid ->
        if (bid == null) flowOf(emptyList()) else repo.recentItemNames(bid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var loadedAmountPaid: Double = 0.0
    private var loadedCreatedAt: Long = 0L
    private var loadedSequence: Int = 0

    init {
        if (isEdit) {
            viewModelScope.launch {
                val inv = repo.getInvoice(invoiceId).first() ?: return@launch
                loadedAmountPaid = inv.amountPaid
                loadedCreatedAt = inv.createdAt
                loadedSequence = inv.sequenceNumber
                issueDate.value = inv.issueDate
                dueDate.value = inv.dueDate
                notes.value = inv.notes.orEmpty()
                discountValueStr.value = if (inv.discountValue % 1.0 == 0.0)
                    inv.discountValue.toLong().toString() else inv.discountValue.toString()
                discountIsPercent.value = inv.discountIsPercent
                val loadedItems = repo.invoiceItems(invoiceId).first()
                if (loadedItems.isNotEmpty()) {
                    items.value = loadedItems.map { DraftItem.from(it) }
                }
                val c = repo.getClient(inv.customerId).first()
                customer.value = c
            }
        }
    }

    val totals: StateFlow<InvoiceTotals> =
        combine(items, discountValueStr, discountIsPercent) { its, dStr, pct ->
            val dummy = Invoice(
                businessId = 0,
                customerId = 0,
                sequenceNumber = 0,
                issueDate = 0,
                discountValue = dStr.toDoubleOrNull() ?: 0.0,
                discountIsPercent = pct,
                amountPaid = loadedAmountPaid
            )
            val itemList = its.map { it.toInvoiceItem(0) }
            InvoiceCalc.compute(dummy, itemList)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            InvoiceTotals(0.0, 0.0, 0.0, 0.0, 0.0, InvoiceStatus.PENDING)
        )

    fun addItem() {
        items.value = items.value + DraftItem(sortOrder = items.value.size)
    }

    fun removeItem(index: Int) {
        items.value = items.value.toMutableList().apply { removeAt(index) }
    }

    fun updateItem(index: Int, updated: DraftItem) {
        items.value = items.value.toMutableList().apply { this[index] = updated }
    }

    fun setCustomer(c: Client) { customer.value = c }

    fun canSave(): Boolean {
        if (customer.value == null) return false
        val its = items.value
        if (its.isEmpty()) return false
        return its.any { it.name.isNotBlank() && it.quantity() > 0.0 && it.unitPrice() >= 0.0 }
    }

    suspend fun save(): Long? {
        val bid = active.id.value ?: return null
        val cust = customer.value ?: return null
        val validItems = items.value
            .filter { it.name.isNotBlank() && it.quantity() > 0.0 && it.unitPrice() >= 0.0 }
        if (validItems.isEmpty()) return null
        val inv = Invoice(
            id = invoiceId,
            businessId = bid,
            customerId = cust.id,
            sequenceNumber = loadedSequence,
            issueDate = issueDate.value,
            dueDate = dueDate.value,
            notes = notes.value.trim().ifBlank { null },
            discountValue = discountValueStr.value.toDoubleOrNull() ?: 0.0,
            discountIsPercent = discountIsPercent.value,
            amountPaid = loadedAmountPaid,
            createdAt = if (loadedCreatedAt == 0L) System.currentTimeMillis() else loadedCreatedAt,
            updatedAt = System.currentTimeMillis()
        )
        return repo.saveInvoice(
            inv,
            validItems.mapIndexed { idx, d -> d.toInvoiceItem(inv.id).copy(sortOrder = idx) }
        )
    }
}
