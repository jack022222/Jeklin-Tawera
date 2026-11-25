package com.example.util

import com.example.data.local.TransactionWithItems
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val indonesiaLocale = Locale("id", "ID")

    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(indonesiaLocale)
        val absValue = kotlin.math.abs(amount)
        val formatted = formatter.format(absValue)
        return if (amount < 0) "-Rp $formatted" else "Rp $formatted"
    }

    fun formatShortRupiah(amount: Long): String {
        val abs = kotlin.math.abs(amount)
        val prefix = if (amount < 0) "-" else ""
        return when {
            abs >= 1_000_000_000L -> "${prefix}Rp ${String.format(indonesiaLocale, "%.1fM", abs / 1_000_000_000.0)}"
            abs >= 1_000_000L -> "${prefix}Rp ${String.format(indonesiaLocale, "%.1fjt", abs / 1_000_000.0)}"
            abs >= 1_000L -> "${prefix}Rp ${abs / 1_000L}rb"
            else -> "${prefix}Rp $abs"
        }
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy, HH:mm", indonesiaLocale).format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        return SimpleDateFormat("EEEE, dd MMM yyyy", indonesiaLocale).format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM", indonesiaLocale).format(Date(timestamp))
    }

    fun formatDayNameShort(timestamp: Long): String {
        return SimpleDateFormat("EEE", indonesiaLocale).format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", indonesiaLocale).format(Date(timestamp))
    }

    fun formatFileTimestamp(timestamp: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(timestamp))
    }

    fun startOfDayMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun addDays(startOfDayMillis: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }

    fun buildShareableReceiptText(txWithItems: TransactionWithItems, storeName: String = "KasirKu"): String {
        val tx = txWithItems.transaction
        val sb = StringBuilder()
        sb.appendLine("*$storeName - STRUK PEMBAYARAN*")
        sb.appendLine("================================")
        sb.appendLine("No. Struk : ${tx.invoiceNumber}")
        sb.appendLine("Tanggal   : ${formatDateTime(tx.timestamp)}")
        sb.appendLine("--------------------------------")
        for (item in txWithItems.items) {
            sb.appendLine(item.productName)
            sb.appendLine(
                "  ${item.quantity} ${item.unit} x ${formatRupiah(item.unitSellingPrice)} = ${formatRupiah(item.lineTotal)}"
            )
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("Subtotal  : ${formatRupiah(tx.subtotal)}")
        if (tx.discountAmount > 0L) {
            sb.appendLine("Diskon    : -${formatRupiah(tx.discountAmount)}")
        }
        sb.appendLine("*TOTAL    : ${formatRupiah(tx.totalAmount)}*")
        sb.appendLine("Tunai     : ${formatRupiah(tx.cashReceived)}")
        sb.appendLine("Kembalian : ${formatRupiah(tx.changeAmount)}")
        if (tx.note.isNotBlank()) {
            sb.appendLine("Catatan   : ${tx.note}")
        }
        sb.appendLine("================================")
        sb.append("Terima kasih telah berbelanja!")
        return sb.toString()
    }
}
