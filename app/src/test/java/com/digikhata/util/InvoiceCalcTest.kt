package com.digikhata.util

import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
import com.digikhata.domain.model.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceCalcTest {

    private fun invoice(
        discountValue: Double = 0.0,
        discountIsPercent: Boolean = false,
        amountPaid: Double = 0.0
    ) = Invoice(
        id = 1,
        businessId = 1,
        customerId = 1,
        sequenceNumber = 1,
        issueDate = 0L,
        discountValue = discountValue,
        discountIsPercent = discountIsPercent,
        amountPaid = amountPaid
    )

    private fun item(qty: Double, price: Double, taxPct: Double = 0.0) =
        InvoiceItem(invoiceId = 1, name = "x", quantity = qty, unitPrice = price, taxPercent = taxPct)

    @Test
    fun `subtotal sums quantity times price`() {
        val totals = InvoiceCalc.compute(
            invoice(),
            listOf(item(2.0, 50.0), item(1.0, 100.0))
        )
        assertEquals(200.0, totals.subtotal, 0.001)
    }

    @Test
    fun `tax is summed per line`() {
        val totals = InvoiceCalc.compute(
            invoice(),
            listOf(item(2.0, 100.0, 10.0), item(1.0, 50.0, 20.0))
        )
        // 2*100*10% = 20; 1*50*20% = 10 => 30
        assertEquals(30.0, totals.totalTax, 0.001)
        // subtotal 250 + tax 30 = 280
        assertEquals(280.0, totals.grandTotal, 0.001)
    }

    @Test
    fun `percent discount applies to subtotal and is capped at 100`() {
        val totals = InvoiceCalc.compute(
            invoice(discountValue = 150.0, discountIsPercent = true),
            listOf(item(1.0, 200.0))
        )
        // cap at 100% => discount = 200
        assertEquals(200.0, totals.discountAmount, 0.001)
        assertEquals(0.0, totals.grandTotal, 0.001)
    }

    @Test
    fun `flat discount cannot exceed subtotal`() {
        val totals = InvoiceCalc.compute(
            invoice(discountValue = 500.0, discountIsPercent = false),
            listOf(item(1.0, 100.0))
        )
        assertEquals(100.0, totals.discountAmount, 0.001)
        assertEquals(0.0, totals.grandTotal, 0.001)
    }

    @Test
    fun `grand total is non-negative`() {
        val totals = InvoiceCalc.compute(
            invoice(discountValue = 100.0),
            listOf(item(1.0, 50.0))
        )
        assertEquals(0.0, totals.grandTotal, 0.001)
        assertEquals(0.0, totals.balance, 0.001)
    }

    @Test
    fun `balance equals grand minus paid`() {
        val totals = InvoiceCalc.compute(
            invoice(amountPaid = 40.0),
            listOf(item(1.0, 100.0))
        )
        assertEquals(60.0, totals.balance, 0.001)
    }

    @Test
    fun `balance floored at zero when overpaid`() {
        val totals = InvoiceCalc.compute(
            invoice(amountPaid = 500.0),
            listOf(item(1.0, 100.0))
        )
        assertEquals(0.0, totals.balance, 0.001)
    }

    @Test
    fun `status is PENDING when nothing paid`() {
        val totals = InvoiceCalc.compute(
            invoice(),
            listOf(item(1.0, 100.0))
        )
        assertEquals(InvoiceStatus.PENDING, totals.status)
    }

    @Test
    fun `status is PARTIAL when some paid`() {
        val totals = InvoiceCalc.compute(
            invoice(amountPaid = 40.0),
            listOf(item(1.0, 100.0))
        )
        assertEquals(InvoiceStatus.PARTIAL, totals.status)
    }

    @Test
    fun `status is PAID when fully paid`() {
        val totals = InvoiceCalc.compute(
            invoice(amountPaid = 100.0),
            listOf(item(1.0, 100.0))
        )
        assertEquals(InvoiceStatus.PAID, totals.status)
    }

    @Test
    fun `status is PENDING when grand total is zero`() {
        val totals = InvoiceCalc.compute(
            invoice(),
            emptyList()
        )
        assertEquals(InvoiceStatus.PENDING, totals.status)
    }

    @Test
    fun `display number pads sequence to four digits`() {
        assertEquals("INV-0007", InvoiceCalc.displayNumber("INV-", 7))
        assertEquals("INV-1234", InvoiceCalc.displayNumber("INV-", 1234))
        assertEquals("INV-12345", InvoiceCalc.displayNumber("INV-", 12345))
    }
}
