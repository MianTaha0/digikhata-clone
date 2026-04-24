package com.digikhata.domain

import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.ExpenseEntry
import com.digikhata.data.entity.Invoice
import com.digikhata.domain.model.ClientBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReportsCalcTest {

    private val utc = TimeZone.getTimeZone("UTC")

    /** Fixed "now" at 2026-04-15 12:00 UTC. */
    private val now: Long = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.APRIL, 15, 12, 0, 0)
    }.timeInMillis

    private fun invoice(issueDate: Long, amountPaid: Double, id: Long = 0): Invoice =
        Invoice(
            id = id,
            businessId = 1L,
            customerId = 1L,
            sequenceNumber = 1,
            issueDate = issueDate,
            amountPaid = amountPaid
        )

    private fun cash(amount: Double, type: Int): CashEntry =
        CashEntry(
            businessId = 1L,
            amount = amount,
            type = type,
            category = "",
            entryDate = now
        )

    private fun expense(amount: Double, entryDate: Long): ExpenseEntry =
        ExpenseEntry(
            businessId = 1L,
            amount = amount,
            category = "Food",
            paymentMethod = "Cash",
            entryDate = entryDate
        )

    private fun client(id: Long, name: String): Client =
        Client(id = id, businessId = 1L, type = 0, name = name)

    private fun daysAgo(n: Int): Long = now - n * 24L * 60 * 60 * 1000

    @Test
    fun `empty inputs produce zeroed summary with 30 bucket chart`() {
        val s = ReportsCalc.compute(emptyList(), emptyList(), emptyList(), emptyList(), now, utc)
        assertEquals(0.0, s.salesThisMonth, 0.0001)
        assertEquals(0.0, s.expensesThisMonth, 0.0001)
        assertEquals(0.0, s.cashInHand, 0.0001)
        assertTrue(s.topClients.isEmpty())
        assertEquals(30, s.last30DaysSales.size)
        assertTrue(s.last30DaysSales.all { it == 0.0 })
    }

    @Test
    fun `salesThisMonth includes only invoices on or after start of month`() {
        val monthStart = ReportsCalc.startOfMonth(now, utc)
        val lastMonth = monthStart - 5L * 24 * 60 * 60 * 1000
        val invs = listOf(
            invoice(monthStart, 100.0),
            invoice(now, 200.0),
            invoice(lastMonth, 999.0) // excluded
        )
        val s = ReportsCalc.compute(invs, emptyList(), emptyList(), emptyList(), now, utc)
        assertEquals(300.0, s.salesThisMonth, 0.0001)
    }

    @Test
    fun `expensesThisMonth filters by start of month`() {
        val monthStart = ReportsCalc.startOfMonth(now, utc)
        val lastMonth = monthStart - 1L
        val exps = listOf(
            expense(50.0, monthStart),
            expense(25.0, now),
            expense(1000.0, lastMonth) // excluded
        )
        val s = ReportsCalc.compute(emptyList(), exps, emptyList(), emptyList(), now, utc)
        assertEquals(75.0, s.expensesThisMonth, 0.0001)
    }

    @Test
    fun `cashInHand sums type 1 as positive and type 0 as negative`() {
        val entries = listOf(
            cash(100.0, 1), // cash in
            cash(30.0, 0),  // cash out
            cash(50.0, 1)
        )
        val s = ReportsCalc.compute(emptyList(), emptyList(), entries, emptyList(), now, utc)
        assertEquals(120.0, s.cashInHand, 0.0001)
    }

    @Test
    fun `topClients takes 5 by absolute balance descending and excludes zero`() {
        val bals = listOf(
            ClientBalance(client(1, "A"), 100.0),
            ClientBalance(client(2, "B"), -500.0),
            ClientBalance(client(3, "C"), 0.0),    // excluded
            ClientBalance(client(4, "D"), 250.0),
            ClientBalance(client(5, "E"), -50.0),
            ClientBalance(client(6, "F"), 10.0),
            ClientBalance(client(7, "G"), 999.0)
        )
        val s = ReportsCalc.compute(emptyList(), emptyList(), emptyList(), bals, now, utc)
        assertEquals(5, s.topClients.size)
        // Order by abs desc: G(999), B(-500), D(250), A(100), E(-50)
        assertEquals("G", s.topClients[0].client.name)
        assertEquals("B", s.topClients[1].client.name)
        assertEquals("D", s.topClients[2].client.name)
        assertEquals("A", s.topClients[3].client.name)
        assertEquals("E", s.topClients[4].client.name)
    }

    @Test
    fun `last30DaysSales buckets invoices with today at the last index`() {
        val invs = listOf(
            invoice(now, 10.0),            // today -> index 29
            invoice(daysAgo(1), 20.0),     // index 28
            invoice(daysAgo(29), 30.0),    // index 0
            invoice(daysAgo(30), 99.0)     // outside window
        )
        val s = ReportsCalc.compute(invs, emptyList(), emptyList(), emptyList(), now, utc)
        assertEquals(10.0, s.last30DaysSales[29], 0.0001)
        assertEquals(20.0, s.last30DaysSales[28], 0.0001)
        assertEquals(30.0, s.last30DaysSales[0], 0.0001)
        val total = s.last30DaysSales.sum()
        assertEquals(60.0, total, 0.0001)
    }

    @Test
    fun `last30DaysSales aggregates multiple invoices on same day`() {
        val invs = listOf(
            invoice(now, 10.0),
            invoice(now, 5.0),
            invoice(daysAgo(1), 3.0)
        )
        val s = ReportsCalc.compute(invs, emptyList(), emptyList(), emptyList(), now, utc)
        assertEquals(15.0, s.last30DaysSales[29], 0.0001)
        assertEquals(3.0, s.last30DaysSales[28], 0.0001)
    }

    @Test
    fun `startOfMonth returns midnight of first of month`() {
        val m = ReportsCalc.startOfMonth(now, utc)
        val c = Calendar.getInstance(utc).apply { timeInMillis = m }
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals(0, c.get(Calendar.SECOND))
        assertEquals(0, c.get(Calendar.MILLISECOND))
    }
}
