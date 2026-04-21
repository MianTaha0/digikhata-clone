package com.digikhata.ui.invoice

import com.digikhata.data.entity.Invoice
import com.digikhata.domain.model.InvoiceTotals

data class InvoiceCardData(
    val invoice: Invoice,
    val customerName: String,
    val totals: InvoiceTotals
)
