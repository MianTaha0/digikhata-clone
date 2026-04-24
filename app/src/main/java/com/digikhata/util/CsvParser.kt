package com.digikhata.util

/**
 * Pure CSV parser — RFC 4180. Inverse of [CsvExporter].
 *
 * Handles:
 *  - quoted fields containing commas / quotes / newlines
 *  - double-quote escaping (`""` inside a quoted field)
 *  - CRLF and LF line endings
 *  - a trailing newline without producing a spurious empty row
 */
object CsvParser {

    /** Parse a full CSV document into rows of fields. */
    fun parse(csv: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var i = 0
        val n = csv.length
        val field = StringBuilder()
        val row = ArrayList<String>()
        var inQuotes = false

        while (i < n) {
            val c = csv[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && csv[i + 1] == '"') {
                        field.append('"'); i += 2; continue
                    }
                    inQuotes = false; i++; continue
                }
                field.append(c); i++; continue
            }
            when (c) {
                '"' -> { inQuotes = true; i++ }
                ',' -> { row.add(field.toString()); field.setLength(0); i++ }
                '\r' -> {
                    row.add(field.toString()); field.setLength(0)
                    rows.add(row.toList()); row.clear()
                    i++
                    if (i < n && csv[i] == '\n') i++
                }
                '\n' -> {
                    row.add(field.toString()); field.setLength(0)
                    rows.add(row.toList()); row.clear()
                    i++
                }
                else -> { field.append(c); i++ }
            }
        }
        // Trailing field / row
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row.toList())
        }
        return rows
    }

    /**
     * Parse into a list of maps keyed by the header row.
     * Skips empty rows. Missing trailing cells default to "".
     */
    fun parseAsMaps(csv: String): List<Map<String, String>> {
        val rows = parse(csv).filter { it.any { cell -> cell.isNotEmpty() } }
        if (rows.isEmpty()) return emptyList()
        val headers = rows[0]
        return rows.drop(1).map { r ->
            headers.mapIndexed { idx, h -> h to (r.getOrNull(idx) ?: "") }.toMap()
        }
    }
}
