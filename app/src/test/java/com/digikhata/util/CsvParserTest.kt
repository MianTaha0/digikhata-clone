package com.digikhata.util

import com.digikhata.data.entity.Client
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {

    @Test
    fun `parses simple rows`() {
        val rows = CsvParser.parse("a,b,c\n1,2,3\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), rows)
    }

    @Test
    fun `handles CRLF line endings`() {
        val rows = CsvParser.parse("a,b\r\n1,2\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test
    fun `unquotes quoted fields with commas`() {
        val rows = CsvParser.parse("name,note\n\"Doe, Jane\",hi\n")
        assertEquals(listOf(listOf("name", "note"), listOf("Doe, Jane", "hi")), rows)
    }

    @Test
    fun `unescapes doubled quotes inside quoted fields`() {
        val rows = CsvParser.parse("q\n\"she said \"\"hi\"\"\"\n")
        assertEquals(listOf(listOf("q"), listOf("she said \"hi\"")), rows)
    }

    @Test
    fun `preserves newlines inside quoted fields`() {
        val rows = CsvParser.parse("x\n\"line1\nline2\"\n")
        assertEquals(listOf(listOf("x"), listOf("line1\nline2")), rows)
    }

    @Test
    fun `parseAsMaps keys by header and skips empty lines`() {
        val maps = CsvParser.parseAsMaps("a,b\n1,2\n\n3,4\n")
        assertEquals(
            listOf(
                mapOf("a" to "1", "b" to "2"),
                mapOf("a" to "3", "b" to "4")
            ),
            maps
        )
    }

    @Test
    fun `parseAsMaps fills missing trailing cells with empty string`() {
        val maps = CsvParser.parseAsMaps("a,b,c\n1,2\n")
        assertEquals(listOf(mapOf("a" to "1", "b" to "2", "c" to "")), maps)
    }

    @Test
    fun `round-trips clients through exporter and parser`() {
        val src = listOf(
            Client(id = 5L, businessId = 1L, type = 0, name = "Doe, Jane", phone = "+1 555", address = "line1\nline2"),
            Client(id = 6L, businessId = 1L, type = 1, name = "Acme \"Ltd\"")
        )
        val csv = CsvExporter.clientsCsv(src)
        val maps = CsvParser.parseAsMaps(csv)
        assertEquals(2, maps.size)
        assertEquals("Doe, Jane", maps[0]["name"])
        assertEquals("customer", maps[0]["type"])
        assertEquals("line1\nline2", maps[0]["address"])
        assertEquals("Acme \"Ltd\"", maps[1]["name"])
        assertEquals("supplier", maps[1]["type"])
    }
}
