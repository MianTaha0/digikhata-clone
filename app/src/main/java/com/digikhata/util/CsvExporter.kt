package com.digikhata.util

import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.TxEntity

/**
 * Pure CSV formatting — RFC 4180 quoting. No Android deps, trivially unit-testable.
 */
object CsvExporter {

    /**
     * Escape a single CSV field. Returns "" for null. Values containing quote, comma,
     * CR, or LF are wrapped in double quotes and any internal quotes are doubled.
     */
    fun escape(value: Any?): String {
        if (value == null) return ""
        val s = value.toString()
        return if (s.any { it == '"' || it == ',' || it == '\n' || it == '\r' }) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
    }

    fun toCsv(headers: List<String>, rows: List<List<Any?>>): String {
        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { escape(it) }).append('\n')
        for (row in rows) {
            sb.append(row.joinToString(",") { escape(it) }).append('\n')
        }
        return sb.toString()
    }

    fun clientsCsv(list: List<Client>): String = toCsv(
        headers = listOf("id", "type", "name", "phone", "phone2", "cnic", "address", "creditLimit", "rating", "isPinned", "isArchived", "createdAt", "updatedAt"),
        rows = list.map {
            listOf(it.id, if (it.type == 0) "customer" else "supplier", it.name, it.phone, it.phone2, it.cnic, it.address, it.creditLimit, it.rating, it.isPinned, it.isArchived, it.createdAt, it.updatedAt)
        }
    )

    fun transactionsCsv(list: List<TxEntity>): String = toCsv(
        headers = listOf("id", "clientId", "amount", "type", "notes", "entryDate", "createdAt", "updatedAt"),
        rows = list.map {
            listOf(it.id, it.clientId, it.amount, if (it.type == 0) "gave" else "got", it.notes, it.entryDate, it.createdAt, it.updatedAt)
        }
    )

    fun cashCsv(list: List<CashEntry>): String = toCsv(
        headers = listOf("id", "amount", "type", "category", "note", "entryDate", "createdAt", "updatedAt"),
        rows = list.map {
            listOf(it.id, it.amount, if (it.type == 0) "out" else "in", it.category, it.note, it.entryDate, it.createdAt, it.updatedAt)
        }
    )

    fun expensesCsv(list: List<ExpenseEntry>): String = toCsv(
        headers = listOf("id", "amount", "category", "paymentMethod", "note", "entryDate", "createdAt", "updatedAt"),
        rows = list.map {
            listOf(it.id, it.amount, it.category, it.paymentMethod, it.note, it.entryDate, it.createdAt, it.updatedAt)
        }
    )

    fun invoicesCsv(list: List<Invoice>): String = toCsv(
        headers = listOf("id", "customerId", "sequenceNumber", "issueDate", "dueDate", "notes", "discountValue", "discountIsPercent", "amountPaid", "createdAt", "updatedAt"),
        rows = list.map {
            listOf(it.id, it.customerId, it.sequenceNumber, it.issueDate, it.dueDate, it.notes, it.discountValue, it.discountIsPercent, it.amountPaid, it.createdAt, it.updatedAt)
        }
    )
}
