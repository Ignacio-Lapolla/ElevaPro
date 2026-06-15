package com.grupo.elevapro.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.grupo.elevapro.data.model.domain.Factura
import com.grupo.elevapro.data.model.domain.Orden
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PAGE_W = 595   // A4 a 72 dpi
private const val PAGE_H = 842
private const val MARGIN = 48f
private const val LINE_H = 20f

object PdfGenerator {

    fun generarOrden(context: Context, orden: Orden): Uri {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        drawOrden(page.canvas, orden)
        doc.finishPage(page)
        val uri = guardar(context, doc, "orden_${orden.numero}.pdf")
        doc.close()
        return uri
    }

    fun generarFactura(context: Context, factura: Factura): Uri {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        drawFactura(page.canvas, factura)
        doc.finishPage(page)
        val uri = guardar(context, doc, "factura_${factura.numero}.pdf")
        doc.close()
        return uri
    }

    fun abrir(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ── dibujo ──────────────────────────────────────────────────────────────

    private fun drawOrden(canvas: Canvas, orden: Orden) {
        val bold = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 18f; color = Color.BLACK }
        val title = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 14f; color = Color.BLACK }
        val body  = Paint().apply { textSize = 12f; color = Color.DKGRAY }
        val small = Paint().apply { textSize = 10f; color = Color.GRAY }

        var y = MARGIN + 24f

        // Cabecera
        canvas.drawText("ElevaPro", MARGIN, y, bold); y += LINE_H + 4
        canvas.drawText("ORDEN DE TRABAJO", MARGIN, y, title); y += LINE_H
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, small); y += LINE_H

        // Datos
        fun row(label: String, value: String) {
            canvas.drawText("$label:", MARGIN, y, small)
            canvas.drawText(value, MARGIN + 120f, y, body)
            y += LINE_H + 2
        }

        row("Número", orden.numero)
        row("Fecha", orden.fecha)
        row("Cliente", orden.clienteNombre)
        row("Tipo de trabajo", orden.tipo)
        if (orden.observaciones.isNotBlank()) row("Observaciones", orden.observaciones)

        y += LINE_H
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, small); y += LINE_H

        // Firma
        canvas.drawText("FIRMA DEL CLIENTE", MARGIN, y, title); y += LINE_H + 4
        if (orden.firmada) {
            canvas.drawText("Firmada por: ${orden.nombreFirmante ?: "—"}", MARGIN, y, body)
        } else {
            canvas.drawText("Pendiente de firma", MARGIN, y, body)
        }

        // Pie
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generado: $fecha", MARGIN, PAGE_H - MARGIN, small)
    }

    private fun drawFactura(canvas: Canvas, factura: Factura) {
        val bold = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 18f; color = Color.BLACK }
        val title = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 14f; color = Color.BLACK }
        val body  = Paint().apply { textSize = 12f; color = Color.DKGRAY }
        val small = Paint().apply { textSize = 10f; color = Color.GRAY }

        var y = MARGIN + 24f

        canvas.drawText("ElevaPro", MARGIN, y, bold); y += LINE_H + 4
        canvas.drawText("COMPROBANTE — TIPO ${factura.tipo}", MARGIN, y, title); y += LINE_H
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, small); y += LINE_H

        fun row(label: String, value: String) {
            canvas.drawText("$label:", MARGIN, y, small)
            canvas.drawText(value, MARGIN + 140f, y, body)
            y += LINE_H + 2
        }

        row("Número", factura.numero)
        row("Fecha", factura.fecha)
        row("Cliente", factura.clienteNombre)
        row("Importe", "$ ${"%,.2f".format(factura.monto)}")
        row("Estado", factura.estado.name)
        if (!factura.cae.isNullOrBlank()) {
            row("CAE", factura.cae)
            row("Vencimiento CAE", factura.vencimientoCae ?: "—")
        }

        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generado: $fecha", MARGIN, PAGE_H - MARGIN, small)
    }

    // ── I/O ──────────────────────────────────────────────────────────────────

    private fun guardar(context: Context, doc: PdfDocument, nombre: String): Uri {
        val dir = File(context.getExternalFilesDir("PDFs"), "").also { it.mkdirs() }
        val file = File(dir, nombre)
        FileOutputStream(file).use { doc.writeTo(it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
