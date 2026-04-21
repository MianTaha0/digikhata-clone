package com.digikhata.domain.model

import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem

enum class InvoiceStatus { PAID, PARTIAL, PENDING }

data class InvoiceTotals(
    val subtotal: Double,
    val totalTax: Double,
    val discountAmount: Double,
    val grandTotal: Double,
    val balance: Double,
    val status: InvoiceStatus
)

data class InvoiceWithItems(val invoice: Invoice, val items: List<InvoiceItem>)
