package id.dompetbersama.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset

class CsvExporterTest {

    @Test
    fun escapesCommasAndQuotes() {
        assertEquals("\"makan, kopi\"", CsvExporter.escape("makan, kopi"))
        assertEquals("\"kata \"\"enak\"\"\"", CsvExporter.escape("kata \"enak\""))
    }

    @Test
    fun neutralisesSpreadsheetFormulas() {
        assertEquals("'=SUM(A1)", CsvExporter.escape("=SUM(A1)"))
        assertEquals("'@cmd", CsvExporter.escape("@cmd"))
        assertEquals("'\t=1+1", CsvExporter.escape("\t=1+1"))
        assertEquals("' =1+1", CsvExporter.escape(" =1+1"))
        assertEquals("'＝1+1", CsvExporter.escape("＝1+1"))
    }

    @Test
    fun writesHeaderAndRows() {
        val tx = MoneyTransaction(
            id = 1,
            type = TxType.EXPENSE,
            amount = 25_000,
            category = "Makan",
            note = "nasi, teh",
            occurredAt = 0L,
        )
        val out = ByteArrayOutputStream()
        CsvExporter.write(out, listOf(tx), ZoneOffset.UTC)
        val lines = out.toString(Charsets.UTF_8).removePrefix("﻿").trim().lines()

        assertEquals("tanggal,tipe,nominal,kategori,sumber,catatan", lines[0])
        assertEquals("1970-01-01 00:00,keluar,25000,Makan,Manual,\"nasi, teh\"", lines[1])
        assertTrue(lines.size == 2)
    }
}
