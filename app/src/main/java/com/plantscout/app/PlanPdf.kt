package com.plantscout.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Renders a CustomerPlan as a US-Letter PDF with the company letterhead. */
object PlanPdf {

    private const val W = 612
    private const val H = 792
    private const val M = 48f
    private const val FOOTER = 34f

    private class Ctx(
        val doc: PdfDocument,
        val branding: Branding?,
        val logo: Bitmap?,
        val primary: Int,
        val plan: CustomerPlan
    ) {
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var pageNum = 0
        var y = M
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false, italic: Boolean = false) = TextPaint().apply {
        isAntiAlias = true
        textSize = size
        this.color = color
        typeface = Typeface.create(
            Typeface.SANS_SERIF,
            when {
                bold && italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
        )
    }

    private val dark = Color.rgb(34, 34, 34)
    private val grey = Color.rgb(95, 99, 104)

    fun export(context: Context, plan: CustomerPlan, branding: Branding?): File {
        val primary = try {
            Color.parseColor(branding?.primaryColor?.ifBlank { null } ?: "#28734b")
        } catch (e: Exception) {
            Color.rgb(40, 115, 75)
        }
        val logo = branding?.logoPath?.let { p -> if (File(p).exists()) BitmapFactory.decodeFile(p) else null }
        val c = Ctx(PdfDocument(), branding, logo, primary, plan)

        newPage(c, first = true)
        drawTitleBlock(c)
        for (s in plan.sections) drawSection(c, s)
        finishPage(c)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = (plan.customer.name.ifBlank { "Customer" } + " " + plan.planNumber)
            .replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(' ', '_')
        val out = File(dir, "Eradication_Plan_$safeName.pdf")
        out.outputStream().use { c.doc.writeTo(it) }
        c.doc.close()
        return out
    }

    // ---------------- Pages ----------------

    private fun newPage(c: Ctx, first: Boolean) {
        finishPage(c)
        c.pageNum++
        val p = c.doc.startPage(PdfDocument.PageInfo.Builder(W, H, c.pageNum).create())
        c.page = p
        c.canvas = p.canvas
        c.y = if (first) drawLetterhead(c) else drawRunningHeader(c)
    }

    private fun finishPage(c: Ctx) {
        val p = c.page ?: return
        val cv = p.canvas
        val line = Paint().apply { color = c.primary; strokeWidth = 0.8f }
        cv.drawLine(M, H - FOOTER, W - M, H - FOOTER, line)
        val small = paint(8f, grey)
        val left = listOf(c.branding?.company, c.branding?.phone, c.branding?.website)
            .filter { !it.isNullOrBlank() }.joinToString("  ·  ")
        cv.drawText(left, M, H - FOOTER + 13f, small)
        val right = "${c.plan.planNumber}  ·  Page ${c.pageNum}"
        cv.drawText(right, W - M - small.measureText(right), H - FOOTER + 13f, small)
        c.doc.finishPage(p)
        c.page = null
        c.canvas = null
    }

    private fun drawLetterhead(c: Ctx): Float {
        val cv = c.canvas!!
        val b = c.branding
        var logoRight = M
        val logoH = 64f
        c.logo?.let { bmp ->
            val w = logoH * bmp.width / bmp.height.coerceAtLeast(1)
            val drawW = w.coerceAtMost(170f)
            val drawH = drawW * bmp.height / bmp.width.coerceAtLeast(1)
            cv.drawBitmap(bmp, null, RectF(M, M, M + drawW, M + drawH), Paint(Paint.FILTER_BITMAP_FLAG))
            logoRight = M + drawW + 14f
        }

        // Right-aligned company details
        val nameP = paint(15f, c.primary, bold = true)
        val detailP = paint(8.8f, grey)
        val right = W - M
        var ty = M + 14f
        val name = b?.company?.ifBlank { null } ?: ""
        if (name.isNotEmpty()) {
            cv.drawText(name, right - nameP.measureText(name), ty, nameP)
            ty += 14f
        }
        val lines = mutableListOf<String>()
        b?.tagline?.takeIf { it.isNotBlank() }?.let { lines += it }
        b?.address?.takeIf { it.isNotBlank() }?.let { lines += it }
        listOf(b?.phone, b?.email).filter { !it.isNullOrBlank() }.joinToString("  ·  ").takeIf { it.isNotBlank() }?.let { lines += it }
        b?.website?.takeIf { it.isNotBlank() }?.let { lines += it }
        for (l in lines) {
            val maxW = right - logoRight
            val text = if (detailP.measureText(l) > maxW) ellipsize(l, detailP, maxW) else l
            cv.drawText(text, right - detailP.measureText(text), ty, detailP)
            ty += 11.5f
        }

        val bottom = maxOf(M + (if (c.logo != null) logoH else 0f), ty) + 8f
        val rule = Paint().apply { color = c.primary; strokeWidth = 2.2f }
        cv.drawLine(M, bottom, W - M, bottom, rule)
        return bottom + 22f
    }

    private fun drawRunningHeader(c: Ctx): Float {
        val cv = c.canvas!!
        val p = paint(9f, c.primary, bold = true)
        val left = c.branding?.company?.ifBlank { null } ?: c.plan.title
        cv.drawText(left, M, M, p)
        val g = paint(9f, grey)
        val right = "${c.plan.title} — ${c.plan.customer.name}"
        cv.drawText(ellipsize(right, g, 300f), W - M - minOf(g.measureText(right), 300f), M, g)
        val line = Paint().apply { color = c.primary; strokeWidth = 0.8f }
        cv.drawLine(M, M + 7f, W - M, M + 7f, line)
        return M + 26f
    }

    private fun ellipsize(text: String, p: Paint, maxW: Float): String {
        if (p.measureText(text) <= maxW) return text
        var t = text
        while (t.isNotEmpty() && p.measureText("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    // ---------------- Content ----------------

    private fun ensure(c: Ctx, height: Float) {
        if (c.y + height > H - FOOTER - 12f) newPage(c, first = false)
    }

    private fun drawTitleBlock(c: Ctx) {
        val plan = c.plan
        val cv = c.canvas!!
        val titleP = paint(21f, dark, bold = true)
        cv.drawText(plan.title, M, c.y, titleP)
        c.y += 16f
        val sub = paint(9.5f, grey)
        val date = DateFormat.getDateInstance(DateFormat.LONG).format(Date(plan.updatedAt))
        cv.drawText("Plan ${plan.planNumber}  ·  $date", M, c.y, sub)
        c.y += 14f

        // Customer box
        val cu = plan.customer
        val rows = mutableListOf<Pair<String, String>>()
        rows += "Prepared for" to listOf(cu.name, cu.company).filter { it.isNotBlank() }.joinToString(" — ")
        if (cu.address.isNotBlank()) rows += "Property" to cu.address
        val contact = listOf(cu.phone, cu.email).filter { it.isNotBlank() }.joinToString("  ·  ")
        if (contact.isNotBlank()) rows += "Contact" to contact
        val job = mutableListOf<String>()
        if (cu.jobNumber.isNotBlank()) job += "Job ${cu.jobNumber}"
        if (cu.acres > 0) job += "${trimNum(cu.acres)} acres"
        if (job.isNotEmpty()) rows += "Job" to job.joinToString("  ·  ")

        val labelP = paint(8.5f, grey, bold = true)
        val valueP = paint(10f, dark)
        val boxTop = c.y
        val rowH = 15f
        val boxH = 12f + rows.size * rowH
        val bg = Paint().apply { color = blend(c.primary, 0.08f) }
        cv.drawRoundRect(RectF(M, boxTop, W - M, boxTop + boxH), 6f, 6f, bg)
        var ry = boxTop + 17f
        for ((label, value) in rows) {
            cv.drawText(label.uppercase(), M + 12f, ry, labelP)
            cv.drawText(ellipsize(value, valueP, W - 2 * M - 110f), M + 100f, ry, valueP)
            ry += rowH
        }
        c.y = boxTop + boxH + 22f
    }

    private fun drawSection(c: Ctx, s: PlanSection) {
        val headP = paint(13.5f, c.primary, bold = true)
        ensure(c, 48f)
        c.canvas!!.drawText(ellipsize(s.title, headP, W - 2 * M), M, c.y, headP)
        c.y += 6f
        val rule = Paint().apply { color = blend(c.primary, 0.35f); strokeWidth = 0.7f }
        c.canvas!!.drawLine(M, c.y, W - M, c.y, rule)
        c.y += 14f

        val bodyP = paint(10.3f, dark)
        val subP = paint(10.3f, dark, bold = true)
        for (raw in s.body.split('\n')) {
            val line = raw.trimEnd()
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> c.y += 5f
                trimmed.endsWith(":") && trimmed.length <= 70 -> {
                    ensure(c, 30f) // keep a sub-heading with at least one line after it
                    paragraph(c, trimmed, subP, 0f)
                    c.y += 1f
                }
                trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    paragraph(c, trimmed.substring(2).trim(), bodyP, 12f, bullet = true)
                }
                else -> paragraph(c, trimmed, bodyP, 0f)
            }
        }
        c.y += 12f
    }

    private fun paragraph(c: Ctx, text: String, p: TextPaint, indent: Float, bullet: Boolean = false) {
        val width = (W - 2 * M - indent).toInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.18f)
            .setIncludePad(false)
            .build()
        for (i in 0 until layout.lineCount) {
            val top = layout.getLineTop(i)
            val lineH = (layout.getLineBottom(i) - top).toFloat()
            ensure(c, lineH)
            val baseline = c.y + (layout.getLineBaseline(i) - top)
            val cv = c.canvas!!
            if (bullet && i == 0) cv.drawText("•", M + 2f, baseline, p)
            var end = layout.getLineEnd(i)
            val start = layout.getLineStart(i)
            while (end > start && text[end - 1] == '\n') end--
            cv.drawText(text, start, end, M + indent, baseline, p)
            c.y += lineH
        }
        c.y += 3f
    }

    private fun blend(color: Int, amount: Float): Int {
        val r = (Color.red(color) * amount + 255 * (1 - amount)).toInt()
        val g = (Color.green(color) * amount + 255 * (1 - amount)).toInt()
        val b = (Color.blue(color) * amount + 255 * (1 - amount)).toInt()
        return Color.rgb(r, g, b)
    }

    private fun trimNum(d: Double): String =
        if (d == Math.floor(d)) d.toLong().toString() else String.format(java.util.Locale.US, "%.2f", d)
}
