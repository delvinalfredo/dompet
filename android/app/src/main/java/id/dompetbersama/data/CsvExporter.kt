package id.dompetbersama.data

import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** Writes without closing [out]; the caller owns the stream. */
    fun write(out: OutputStream, items: List<MoneyTransaction>, zone: ZoneId = ZoneId.systemDefault()) {
        val writer = out.bufferedWriter(Charsets.UTF_8)
        writer.write("﻿") // BOM so Excel reads UTF-8 correctly
        writer.write("tanggal,tipe,nominal,kategori,sumber,catatan\n")
        for (tx in items) {
            val date = Instant.ofEpochMilli(tx.occurredAt).atZone(zone).format(dateFormat)
            val direction = if (tx.type == TxType.INCOME) "masuk" else "keluar"
            val type = if (tx.isTransfer) "pindah $direction" else direction
            writer.write(listOf(date, type, tx.amount.toString(), escape(tx.category), escape(tx.source), escape(tx.note)).joinToString(","))
            writer.write("\n")
        }
        writer.flush()
    }

    internal fun escape(value: String): String {
        // A leading = + - @ (also after spaces, or full-width) makes spreadsheets run the cell as a formula.
        val first = value.trimStart(' ').firstOrNull()
        val safe = if (first in FORMULA_CHARS) "'$value" else value
        val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    private val FORMULA_CHARS = setOf('=', '+', '-', '@', '\t', '\r', '＝', '＋', '－', '＠')
}
