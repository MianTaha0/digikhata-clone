package com.digikhata.util

import com.digikhata.data.entity.TxEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerCalcTest {

    private fun tx(amount: Double, type: Int, entryDate: Long = 0L): TxEntity =
        TxEntity(
            clientId = 1L,
            businessId = 1L,
            amount = amount,
            type = type,
            entryDate = entryDate
        )

    @Test
    fun `empty list returns empty balances`() {
        val out = LedgerCalc.computeRunningBalance(0.0, emptyList())
        assertEquals(0, out.size)
    }

    @Test
    fun `type 0 adds to balance`() {
        val out = LedgerCalc.computeRunningBalance(
            openingBalance = 0.0,
            transactions = listOf(tx(100.0, 0), tx(50.0, 0))
        )
        assertEquals(listOf(100.0, 150.0), out)
    }

    @Test
    fun `type 1 subtracts from balance`() {
        val out = LedgerCalc.computeRunningBalance(
            openingBalance = 0.0,
            transactions = listOf(tx(40.0, 1), tx(10.0, 1))
        )
        assertEquals(listOf(-40.0, -50.0), out)
    }

    @Test
    fun `mixed types produce correct running balance`() {
        val out = LedgerCalc.computeRunningBalance(
            openingBalance = 200.0,
            transactions = listOf(
                tx(100.0, 0),  // +100 -> 300
                tx(50.0, 1),   // -50  -> 250
                tx(30.0, 0),   // +30  -> 280
                tx(280.0, 1)   // -280 -> 0
            )
        )
        assertEquals(listOf(300.0, 250.0, 280.0, 0.0), out)
    }

    @Test
    fun `opening balance carries through`() {
        val out = LedgerCalc.computeRunningBalance(
            openingBalance = -500.0,
            transactions = listOf(tx(100.0, 1))
        )
        assertEquals(listOf(-600.0), out)
    }
}
