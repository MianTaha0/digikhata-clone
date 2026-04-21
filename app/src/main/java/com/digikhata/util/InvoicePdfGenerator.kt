package com.digikhata.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.digikhata.data.entity.Business
import com.digikhata.data.entity.Client
import com.digikhata.data.entity.Invoice
import com.digikhata.data.entity.InvoiceItem
import com.digikhata.domain.model.InvoiceStatus
import com.digikhata.domain.model.InvoiceTotals
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    private val C_RED    = android.graphics.Color.parseColor("#E74425")
    private val C_WHITE  = android.graphics.Color.WHITE
    private val C_ON     = android.graphics.Color.parseColor("#212121")
    private val C_MUTED  = android.graphics.Color.parseColor("#757575")
    private val C_BG     = android.graphics.Color.parseColor("#F5F5F5")
    private val C_LINE   = android.graphics.Color.parseColor("#E0E0E0")
    private val C_GREEN  = android.graphics.Color.parseColor("#2E7D32")

    private const val PW = 595f
    private const val PH = 842f
    private const val M = 40f

    fun generate(
        context: Context,
        business: Business,
        customer: Client,
        invoice: Invoice,
        items: List<InvoiceItem>,
        totals: InvoiceTotals
    ): Uri? = try {
        val doc = PdfDocument()
        val state = DrawState(doc)
        val displayNumber = InvoiceCalc.displayNumber(business.invoicePrefix, invoice.sequenceNumber)
        val currency = business.currency

        state.newPage()
        drawHeader(state.canvas, business, invoice, displayNumber)
        state.y = 120f
        drawBillTo(state.canvas, state, customer)

        drawTableHeader(state.canvas, state)
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        items.forEachIndexed { i, it ->
            if (state.y + 24f > PH - M - 80f) {
                state.newPage()
                drawTableHeader(state.canvas, state)
            }
            drawItemRow(state.canvas, state, i + 1, it, currency, i % 2 == 1)
        }

        if (state.y + 140f > PH - M) {
            state.newPage()
        }
        drawTotals(state.canvas, state, invoice, totals, currency)

        if (totals.status == InvoiceStatus.PAID) {
            drawPaidWatermark(state.canvas)
        }

        drawFooter(state.canvas)
        doc.finishPage(state.page!!)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.also { it.mkdirs() }
        val safe = displayNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "DigiKhata_${safe}.pdf")
        doc.writeTo(FileOutputStream(file))
        doc.close()

        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
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

    private fun drawHeader(c: Canvas, biz: Business, inv: Invoice, displayNumber: String) {
        val bg = Paint().apply { color = C_RED; style = Paint.Style.FILL }
        c.drawRect(0f, 0f, PW, 100f, bg)

        c.drawText(biz.name, M, 38f, paint(C_WHITE, 18f, bold = true))
        val sub = StringBuilder()
        biz.phone?.let { sub.append(it) }
        biz.address?.let { if (sub.isNotEmpty()) sub.append("  •  "); sub.append(it) }
        if (sub.isNotEmpty()) {
            c.drawText(sub.toString(), M, 58f, paint(C_WHITE, 10f).apply { alpha = 220 })
        }

        val rightP = paint(C_WHITE, 12f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("INVOICE", PW - M, 32f, paint(C_WHITE, 18f, bold = true).apply { textAlign = Paint.Align.RIGHT })
        c.drawText(displayNumber, PW - M, 54f, rightP)
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        c.drawText("Issued: ${df.format(Date(inv.issueDate))}", PW - M, 72f, paint(C_WHITE, 9f).apply { textAlign = Paint.Align.RIGHT; alpha = 220 })
        inv.dueDate?.let {
            c.drawText("Due: ${df.format(Date(it))}", PW - M, 86f, paint(C_WHITE, 9f).apply { textAlign = Paint.Align.RIGHT; alpha = 220 })
        }
    }

    private fun drawBillTo(c: Canvas, s: DrawState, customer: Client) {
        c.drawText("BILL TO", M, s.y, paint(C_MUTED, 9f, bold = true))
        s.y += 14f
        c.drawText(customer.name, M, s.y, paint(C_ON, 12f, bold = true))
        s.y += 14f
        customer.phone?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, M, s.y, paint(C_MUTED, 10f))
            s.y += 12f
        }
        customer.address?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, M, s.y, paint(C_MUTED, 10f))
            s.y += 12f
        }
        s.y += 12f
    }

    // Columns
    private val numX = M + 6f
    private val itemX = M + 40f
    private val qtyX = PW - M - 230f
    private val rateX = PW - M - 170f
    private val taxX = PW - M - 100f
    private val totX = PW - M - 6f

    private fun drawTableHeader(c: Canvas, s: DrawState) {
        val bg = Paint().apply { color = C_BG; style = Paint.Style.FILL }
        c.drawRect(M, s.y, PW - M, s.y + 22f, bg)
        val hp = paint(C_MUTED, 9f, bold = true)
        c.drawText("#", numX, s.y + 14f, hp)
        c.drawText("ITEM", itemX, s.y + 14f, hp)
        c.drawText("QTY", qtyX, s.y + 14f, hp)
        c.drawText("RATE", rateX, s.y + 14f, hp)
        c.drawText("TAX%", taxX, s.y + 14f, hp)
        val hpr = paint(C_MUTED, 9f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("TOTAL", totX, s.y + 14f, hpr)
        s.y += 22f
    }

    private fun drawItemRow(
        c: Canvas,
        s: DrawState,
        index: Int,
        it: InvoiceItem,
        currency: String,
        shaded: Boolean
    ) {
        if (shaded) {
            val bg = Paint().apply { color = C_BG; alpha = 60; style = Paint.Style.FILL }
            c.drawRect(M, s.y, PW - M, s.y + 24f, bg)
        }
        val bp = paint(C_ON, 10f)
        val maxNameW = qtyX - itemX - 8f
        var name = it.name
        while (bp.measureText(name) > maxNameW && name.length > 4) {
            name = name.dropLast(4) + "…"
        }
        c.drawText(index.toString(), numX, s.y + 16f, paint(C_MUTED, 10f))
        c.drawText(name, itemX, s.y + 16f, bp)
        c.drawText(fmtNum(it.quantity), qtyX, s.y + 16f, bp)
        c.drawText(CurrencyUtils.format(it.unitPrice, currency), rateX, s.y + 16f, bp)
        c.drawText(fmtNum(it.taxPercent), taxX, s.y + 16f, bp)
        val line = it.quantity * it.unitPrice * (1 + it.taxPercent / 100.0)
        val rp = paint(C_ON, 10f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        c.drawText(CurrencyUtils.format(line, currency), totX, s.y + 16f, rp)
        s.y += 24f
        val lineP = Paint().apply { color = C_LINE; strokeWidth = 0.5f }
        c.drawLine(M, s.y, PW - M, s.y, lineP)
    }

    private fun drawTotals(c: Canvas, s: DrawState, inv: Invoice, t: InvoiceTotals, currency: String) {
        s.y += 14f
        val labelX = PW - M - 170f
        val valueX = PW - M - 6f
        val lp = paint(C_MUTED, 10f)
        val vp = paint(C_ON, 10f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("Subtotal", labelX, s.y, lp)
        c.drawText(CurrencyUtils.format(t.subtotal, currency), valueX, s.y, vp)
        s.y += 16f
        c.drawText("Tax", labelX, s.y, lp)
        c.drawText(CurrencyUtils.format(t.totalTax, currency), valueX, s.y, vp)
        s.y += 16f
        val dLabel = if (inv.discountIsPercent) "Discount (${fmtNum(inv.discountValue)}%)" else "Discount"
        c.drawText(dLabel, labelX, s.y, lp)
        c.drawText("- ${CurrencyUtils.format(t.discountAmount, currency)}", valueX, s.y, vp)
        s.y += 20f

        val lineP = Paint().apply { color = C_LINE; strokeWidth = 0.7f }
        c.drawLine(labelX, s.y - 6f, valueX, s.y - 6f, lineP)

        c.drawText("GRAND TOTAL", labelX, s.y, paint(C_ON, 12f, bold = true))
        c.drawText(CurrencyUtils.format(t.grandTotal, currency), valueX, s.y,
            paint(C_RED, 12f, bold = true).apply { textAlign = Paint.Align.RIGHT })
        s.y += 20f

        if (inv.amountPaid > 0.0) {
            c.drawText("Paid", labelX, s.y, lp)
            c.drawText(CurrencyUtils.format(inv.amountPaid, currency), valueX, s.y, vp)
            s.y += 16f
            c.drawText("BALANCE", labelX, s.y, paint(C_ON, 10f, bold = true))
            c.drawText(CurrencyUtils.format(t.balance, currency), valueX, s.y,
                paint(C_ON, 10f, bold = true).apply { textAlign = Paint.Align.RIGHT })
            s.y += 16f
        }
    }

    private fun drawPaidWatermark(c: Canvas) {
        c.save()
        c.rotate(-20f, PW / 2f, PH / 2f)
        val p = Paint().apply {
            color = C_GREEN
            alpha = 50
            textSize = 100f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        c.drawText("PAID", PW / 2f, PH / 2f + 30f, p)
        c.restore()
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

    private fun fmtNum(d: Double): String =
        if (d % 1.0 == 0.0) d.toLong().toString() else String.format("%.2f", d)

    private fun paint(color: Int, size: Float, bold: Boolean = false) = Paint().apply {
        this.color = color
        this.textSize = size
        this.isFakeBoldText = bold
        this.isAntiAlias = true
    }
}
