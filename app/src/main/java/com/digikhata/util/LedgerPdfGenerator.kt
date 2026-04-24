package com.digikhata.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.TxEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LedgerPdfGenerator {

    private val C_RED = android.graphics.Color.parseColor("#E74425")
    private val C_WHITE = android.graphics.Color.WHITE
    private val C_ON = android.graphics.Color.parseColor("#212121")
    private val C_MUTED = android.graphics.Color.parseColor("#757575")
    private val C_BG = android.graphics.Color.parseColor("#F5F5F5")
    private val C_LINE = android.graphics.Color.parseColor("#E0E0E0")
    private val C_GREEN = android.graphics.Color.parseColor("#2E7D32")

    private const val PW = 595f
    private const val PH = 842f
    private const val M = 40f

    fun generate(
        context: Context,
        business: Business,
        client: Client,
        transactions: List<TxEntity>,
        openingBalance: Double,
        fromDate: Long,
        toDate: Long,
        currency: String
    ): File? = try {
        val doc = PdfDocument()
        val state = DrawState(doc)
        state.newPage()
        drawHeader(state.canvas, business, client, fromDate, toDate)
        state.y = 130f
        drawClient(state.canvas, state, client)

        drawTableHeader(state.canvas, state)
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val runs = LedgerCalc.computeRunningBalance(openingBalance, transactions)
        drawOpeningRow(state.canvas, state, openingBalance, currency)
        transactions.forEachIndexed { i, t ->
            if (state.y + 24f > PH - M - 80f) {
                state.newPage()
                drawTableHeader(state.canvas, state)
            }
            drawTxRow(state.canvas, state, dateFmt, t, runs[i], currency, i % 2 == 1)
        }

        val closing = runs.lastOrNull() ?: openingBalance
        if (state.y + 80f > PH - M) state.newPage()
        drawFooterClosing(state.canvas, state, closing, currency)
        drawFooter(state.canvas)
        doc.finishPage(state.page!!)

        val dir = File(context.cacheDir, "ledgers").also { it.mkdirs() }
        val file = File(dir, "ledger_${client.id}_${fromDate}_${toDate}.pdf")
        doc.writeTo(FileOutputStream(file))
        doc.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private class DrawState(val doc: PdfDocument) {
        var page: PdfDocument.Page? = null
        var canvas: Canvas = Canvas()
        var pageNum = 1
        var y = M

        fun newPage() {
            if (page != null) doc.finishPage(page)
            val info = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), pageNum++).create()
            page = doc.startPage(info)
            canvas = page!!.canvas
            y = M
        }
    }

    private fun drawHeader(c: Canvas, biz: Business, client: Client, from: Long, to: Long) {
        val bg = Paint().apply { color = C_RED; style = Paint.Style.FILL }
        c.drawRect(0f, 0f, PW, 110f, bg)
        c.drawText(biz.name, M, 38f, paint(C_WHITE, 18f, bold = true))
        val sub = StringBuilder()
        biz.phone?.let { sub.append(it) }
        biz.address?.let { if (sub.isNotEmpty()) sub.append("  •  "); sub.append(it) }
        if (sub.isNotEmpty()) {
            c.drawText(sub.toString(), M, 58f, paint(C_WHITE, 10f).apply { alpha = 220 })
        }
        val rightP = paint(C_WHITE, 12f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("LEDGER STATEMENT", PW - M, 32f, paint(C_WHITE, 16f, bold = true).apply { textAlign = Paint.Align.RIGHT })
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        c.drawText("${df.format(Date(from))}  —  ${df.format(Date(to))}", PW - M, 54f, rightP)
        c.drawText(client.name, PW - M, 78f, paint(C_WHITE, 10f).apply { textAlign = Paint.Align.RIGHT; alpha = 220 })
    }

    private fun drawClient(c: Canvas, s: DrawState, client: Client) {
        c.drawText("ACCOUNT", M, s.y, paint(C_MUTED, 9f, bold = true))
        s.y += 14f
        c.drawText(client.name, M, s.y, paint(C_ON, 12f, bold = true))
        s.y += 14f
        client.phone?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, M, s.y, paint(C_MUTED, 10f))
            s.y += 12f
        }
        client.address?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, M, s.y, paint(C_MUTED, 10f))
            s.y += 12f
        }
        s.y += 10f
    }

    private val dateX = M + 6f
    private val descX = M + 90f
    private val debitX = PW - M - 200f
    private val creditX = PW - M - 110f
    private val balX = PW - M - 6f

    private fun drawTableHeader(c: Canvas, s: DrawState) {
        val bg = Paint().apply { color = C_BG; style = Paint.Style.FILL }
        c.drawRect(M, s.y, PW - M, s.y + 22f, bg)
        val hp = paint(C_MUTED, 9f, bold = true)
        val hpr = paint(C_MUTED, 9f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("DATE", dateX, s.y + 14f, hp)
        c.drawText("DESCRIPTION", descX, s.y + 14f, hp)
        c.drawText("DEBIT", debitX, s.y + 14f, hpr)
        c.drawText("CREDIT", creditX, s.y + 14f, hpr)
        c.drawText("BALANCE", balX, s.y + 14f, hpr)
        s.y += 22f
    }

    private fun drawOpeningRow(c: Canvas, s: DrawState, opening: Double, currency: String) {
        val bp = paint(C_MUTED, 10f, bold = true)
        c.drawText("—", dateX, s.y + 16f, bp)
        c.drawText("Opening balance", descX, s.y + 16f, bp)
        val rp = paint(C_ON, 10f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        c.drawText(CurrencyUtils.format(opening, currency), balX, s.y + 16f, rp)
        s.y += 24f
        val lineP = Paint().apply { color = C_LINE; strokeWidth = 0.5f }
        c.drawLine(M, s.y, PW - M, s.y, lineP)
    }

    private fun drawTxRow(
        c: Canvas,
        s: DrawState,
        df: SimpleDateFormat,
        t: TxEntity,
        runningBalance: Double,
        currency: String,
        shaded: Boolean
    ) {
        if (shaded) {
            val bg = Paint().apply { color = C_BG; alpha = 60; style = Paint.Style.FILL }
            c.drawRect(M, s.y, PW - M, s.y + 24f, bg)
        }
        val bp = paint(C_ON, 10f)
        c.drawText(df.format(Date(t.entryDate)), dateX, s.y + 16f, bp)
        val maxDescW = debitX - descX - 16f
        var desc = t.notes?.takeIf { it.isNotBlank() } ?: (if (t.type == 0) "You gave" else "You got")
        while (bp.measureText(desc) > maxDescW && desc.length > 4) {
            desc = desc.dropLast(4) + "…"
        }
        c.drawText(desc, descX, s.y + 16f, bp)
        val rp = paint(C_ON, 10f).apply { textAlign = Paint.Align.RIGHT }
        if (t.type == 0) {
            c.drawText(CurrencyUtils.format(t.amount, currency), debitX, s.y + 16f, rp)
        } else {
            c.drawText(CurrencyUtils.format(t.amount, currency), creditX, s.y + 16f, rp)
        }
        val balP = paint(if (runningBalance >= 0) C_ON else C_RED, 10f, bold = true)
            .apply { textAlign = Paint.Align.RIGHT }
        c.drawText(CurrencyUtils.format(runningBalance, currency), balX, s.y + 16f, balP)
        s.y += 24f
        val lineP = Paint().apply { color = C_LINE; strokeWidth = 0.5f }
        c.drawLine(M, s.y, PW - M, s.y, lineP)
    }

    private fun drawFooterClosing(c: Canvas, s: DrawState, closing: Double, currency: String) {
        s.y += 20f
        val labelX = PW - M - 200f
        val valueX = PW - M - 6f
        val lineP = Paint().apply { color = C_LINE; strokeWidth = 0.7f }
        c.drawLine(labelX, s.y - 6f, valueX, s.y - 6f, lineP)
        c.drawText("CLOSING BALANCE", labelX, s.y, paint(C_ON, 12f, bold = true))
        val color = if (closing >= 0) C_GREEN else C_RED
        c.drawText(
            CurrencyUtils.format(closing, currency),
            valueX,
            s.y,
            paint(color, 12f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        )
        s.y += 20f
    }

    private fun drawFooter(c: Canvas) {
        val ts = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val fp = Paint().apply {
            color = C_MUTED
            textSize = 8f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            alpha = 180
        }
        c.drawText("Generated by DigiKhata  •  $ts", PW / 2f, PH - 20f, fp)
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false) = Paint().apply {
        this.color = color
        this.textSize = size
        this.isFakeBoldText = bold
        this.isAntiAlias = true
    }
}
