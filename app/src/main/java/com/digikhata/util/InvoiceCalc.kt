package com.digikhata.util

import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
import com.digikhata.domain.model.InvoiceStatus
import com.digikhata.domain.model.InvoiceTotals

object InvoiceCalc {
    fun compute(invoice: Invoice, items: List<InvoiceItem>): InvoiceTotals {
        val subtotal = items.sumOf { it.quantity * it.unitPrice }
        val totalTax = items.sumOf { it.quantity * it.unitPrice * it.taxPercent / 100.0 }
        val discount = if (invoice.discountIsPercent)
            subtotal * invoice.discountValue.coerceAtMost(100.0) / 100.0
        else
            invoice.discountValue.coerceAtMost(subtotal)
        val grand = (subtotal + totalTax - discount).coerceAtLeast(0.0)
        val balance = (grand - invoice.amountPaid).coerceAtLeast(0.0)
        val status = when {
            invoice.amountPaid >= grand - 0.001 && grand > 0.0 -> InvoiceStatus.PAID
            invoice.amountPaid > 0.0 -> InvoiceStatus.PARTIAL
            else -> InvoiceStatus.PENDING
        }
        return InvoiceTotals(subtotal, totalTax, discount, grand, balance, status)
    }

    fun displayNumber(prefix: String, sequence: Int): String =
        "$prefix${sequence.toString().padStart(4, '0')}"
}
