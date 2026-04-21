package com.digikhata.ui.invoice

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digikhata.ActiveBookHolder
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
import com.digikhata.domain.model.InvoiceStatus
import com.digikhata.domain.model.InvoiceTotals
import com.digikhata.domain.repository.DigiRepository
import com.digikhata.util.InvoiceCalc
import com.digikhata.util.InvoicePdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val repo: DigiRepository,
    private val active: ActiveBookHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val invoiceId: Long = savedStateHandle.get<Long>("invoiceId") ?: 0L

    val invoice: StateFlow<Invoice?> = repo.getInvoice(invoiceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items: StateFlow<List<InvoiceItem>> = repo.invoiceItems(invoiceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customer: StateFlow<Client?> = invoice.flatMapLatest { inv ->
        if (inv == null) flowOf(null) else repo.getClient(inv.customerId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val business: StateFlow<Business?> = active.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currency: StateFlow<String> = active.active
        .map { it?.currency ?: "Pakistan Rupee-Rs" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Pakistan Rupee-Rs")

    val prefix: StateFlow<String> = active.active
        .map { it?.invoicePrefix ?: "INV-" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "INV-")

    val totals: StateFlow<InvoiceTotals> = combine(invoice, items) { inv, its ->
        if (inv == null) InvoiceTotals(0.0, 0.0, 0.0, 0.0, 0.0, InvoiceStatus.PENDING)
        else InvoiceCalc.compute(inv, its)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        InvoiceTotals(0.0, 0.0, 0.0, 0.0, 0.0, InvoiceStatus.PENDING)
    )

    suspend fun recordPayment(amount: Double) {
        if (amount > 0) repo.recordPayment(invoiceId, amount)
    }

    suspend fun delete() {
        invoice.value?.let { repo.deleteInvoice(it) }
    }

    suspend fun generatePdfUri(context: Context): Uri? = withContext(Dispatchers.IO) {
        val inv = invoice.value ?: repo.getInvoice(invoiceId).first() ?: return@withContext null
        val its = items.value.ifEmpty { repo.invoiceItems(invoiceId).first() }
        val cust = customer.value ?: repo.getClient(inv.customerId).first() ?: return@withContext null
        val biz = business.value ?: return@withContext null
        val t = InvoiceCalc.compute(inv, its)
        InvoicePdfGenerator.generate(context, biz, cust, inv, its, t)
    }
}
