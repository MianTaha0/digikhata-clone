package com.digikhata.util

import com.digikhata.data.entity.CashEntry
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.TxEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun `escape returns empty string for null`() {
        assertEquals("", CsvExporter.escape(null))
    }

    @Test
    fun `escape leaves plain text unquoted`() {
        assertEquals("hello", CsvExporter.escape("hello"))
        assertEquals("123", CsvExporter.escape(123))
        assertEquals("true", CsvExporter.escape(true))
    }

    @Test
    fun `escape quotes values with commas`() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
    }

    @Test
    fun `escape doubles internal quotes and wraps`() {
        assertEquals("\"she said \"\"hi\"\"\"", CsvExporter.escape("she said \"hi\""))
    }

    @Test
    fun `escape quotes values with newlines`() {
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"))
        assertEquals("\"line1\rline2\"", CsvExporter.escape("line1\rline2"))
    }

    @Test
    fun `toCsv emits header row then data rows`() {
        val csv = CsvExporter.toCsv(
            headers = listOf("a", "b"),
            rows = listOf(listOf(1, "x"), listOf(2, "y,z"))
        )
        assertEquals("a,b\n1,x\n2,\"y,z\"\n", csv)
    }

    @Test
    fun `clientsCsv contains header and maps type to label`() {
        val csv = CsvExporter.clientsCsv(
            listOf(
                Client(id = 1L, businessId = 1L, type = 0, name = "Alpha"),
                Client(id = 2L, businessId = 1L, type = 1, name = "Beta, Inc.")
            )
        )
        val lines = csv.trim().split("\n")
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("id,type,name"))
        assertTrue(lines[1].contains(",customer,Alpha,"))
        // "Beta, Inc." has a comma -> must be quoted
        assertTrue(lines[2].contains(",supplier,\"Beta, Inc.\","))
    }

    @Test
    fun `transactionsCsv labels type`() {
        val csv = CsvExporter.transactionsCsv(
            listOf(
                TxEntity(id = 1L, clientId = 10L, businessId = 1L, amount = 100.0, type = 0, entryDate = 1000L),
                TxEntity(id = 2L, clientId = 10L, businessId = 1L, amount = 50.0, type = 1, entryDate = 2000L)
            )
        )
        assertTrue(csv.contains(",gave,"))
        assertTrue(csv.contains(",got,"))
    }

    @Test
    fun `cashCsv labels type`() {
        val csv = CsvExporter.cashCsv(
            listOf(
                CashEntry(id = 1L, businessId = 1L, amount = 100.0, type = 1, category = "sale", entryDate = 0L),
                CashEntry(id = 2L, businessId = 1L, amount = 30.0, type = 0, category = "food", entryDate = 0L)
            )
        )
        assertTrue(csv.contains(",in,"))
        assertTrue(csv.contains(",out,"))
    }
}
