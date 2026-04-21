package com.digikhata.ui.invoice

import com.digikhata.data.entity.InvoiceItem

data class DraftItem(
    val id: Long = 0,
    val name: String = "",
    val qtyStr: String = "1",
    val priceStr: String = "",
    val taxStr: String = "0",
    val sortOrder: Int = 0
) {
    fun quantity(): Double = qtyStr.toDoubleOrNull() ?: 0.0
    fun unitPrice(): Double = priceStr.toDoubleOrNull() ?: 0.0
    fun taxPercent(): Double = (taxStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)

    fun toInvoiceItem(invoiceId: Long): InvoiceItem = InvoiceItem(
        id = id,
        invoiceId = invoiceId,
        name = name.trim(),
        quantity = quantity(),
        unitPrice = unitPrice(),
        taxPercent = taxPercent(),
        sortOrder = sortOrder
    )

    companion object {
        fun from(item: InvoiceItem): DraftItem {
            fun dbl(d: Double): String =
                if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
            return DraftItem(
                id = item.id,
                name = item.name,
                qtyStr = dbl(item.quantity),
                priceStr = dbl(item.unitPrice),
                taxStr = dbl(item.taxPercent),
                sortOrder = item.sortOrder
            )
        }
    }
}
