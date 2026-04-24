package com.digikhata.data.reminders

import com.digikhata.data.reminders.DueInvoiceReminders.DueCandidate
import com.digikhata.data.reminders.DueInvoiceReminders.DueStatus
import com.digikhata.data.reminders.DueInvoiceReminders.pickDueInvoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DueInvoiceRemindersTest {

    private val now: Long = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun c(
        id: Long,
        due: Long?,
        paid: Double = 0.0,
        total: Double = 100.0,
        name: String = "Client $id"
    ) = DueCandidate(
        invoiceId = id,
        sequenceNumber = id.toInt(),
        customerId = 10L,
        customerName = name,
        dueDate = due,
        amountPaid = paid,
        grandTotal = total
    )

    @Test
    fun `empty input returns empty`() {
        assertTrue(pickDueInvoices(emptyList(), now).isEmpty())
    }

    @Test
    fun `null due date is skipped`() {
        val hits = pickDueInvoices(listOf(c(1, null)), now)
        assertTrue(hits.isEmpty())
    }

    @Test
    fun `fully paid invoice is skipped even if overdue`() {
        val hits = pickDueInvoices(listOf(c(1, now - 10 * day, paid = 100.0, total = 100.0)), now)
        assertTrue(hits.isEmpty())
    }

    @Test
    fun `zero total invoice is skipped`() {
        val hits = pickDueInvoices(listOf(c(1, now - day, total = 0.0)), now)
        assertTrue(hits.isEmpty())
    }

    @Test
    fun `overdue invoice is reported with days count`() {
        val hits = pickDueInvoices(listOf(c(1, now - 5 * day)), now)
        assertEquals(1, hits.size)
        assertEquals(DueStatus.OVERDUE, hits[0].status)
        assertEquals(5, hits[0].days)
    }

    @Test
    fun `due within window is reported as DUE_SOON`() {
        val hits = pickDueInvoices(listOf(c(1, now + 2 * day)), now, windowDays = 3)
        assertEquals(1, hits.size)
        assertEquals(DueStatus.DUE_SOON, hits[0].status)
        assertEquals(2, hits[0].days)
    }

    @Test
    fun `due outside window is skipped`() {
        val hits = pickDueInvoices(listOf(c(1, now + 10 * day)), now, windowDays = 3)
        assertTrue(hits.isEmpty())
    }

    @Test
    fun `due today has 0 days and is included`() {
        val hits = pickDueInvoices(listOf(c(1, now + 60 * 1000)), now, windowDays = 3)
        assertEquals(1, hits.size)
        assertEquals(DueStatus.DUE_SOON, hits[0].status)
        assertEquals(0, hits[0].days)
    }

    @Test
    fun `partially paid invoice still reports if balance remains`() {
        val hits = pickDueInvoices(
            listOf(c(1, now - day, paid = 40.0, total = 100.0)),
            now
        )
        assertEquals(1, hits.size)
    }

    @Test
    fun `sort puts overdue first sorted by days desc, then due-soon by days asc`() {
        val hits = pickDueInvoices(
            listOf(
                c(1, now + 2 * day, name = "DueSoon2"),
                c(2, now - 10 * day, name = "Overdue10"),
                c(3, now + 1 * day, name = "DueSoon1"),
                c(4, now - 1 * day, name = "Overdue1")
            ),
            now,
            windowDays = 7
        )
        val names = hits.map { it.candidate.customerName }
        assertEquals(listOf("Overdue10", "Overdue1", "DueSoon1", "DueSoon2"), names)
    }
}
