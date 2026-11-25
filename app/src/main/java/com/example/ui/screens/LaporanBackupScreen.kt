package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.BackupImportMode
import com.example.ui.DayBarStat
import com.example.ui.PosUiState
import com.example.ui.components.PosScreenHeader
import com.example.ui.theme.MonoPriceStyle
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanBackupScreen(
    uiState: PosUiState,
    onShiftReportDate: (Int) -> Unit,
    onSelectReportDate: (Long) -> Unit,
    onGenerateJsonBackup: suspend () -> String,
    onGenerateProductsCsv: suspend () -> String,
    onGenerateTransactionsCsv: suspend () -> String,
    onJsonFilePicked: (String) -> Unit,
    onDismissJsonImport: () -> Unit,
    onConfirmJsonImport: (BackupImportMode) -> Unit,
    onCsvProductsPicked: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = writeTextToUri(context, uri, onGenerateJsonBackup())
                onShowMessage(
                    if (ok) "Backup JSON berhasil disimpan!" else "Gagal menulis file backup JSON."
                )
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = readTextFromUri(context, uri)
                if (text != null) {
                    onJsonFilePicked(text)
                } else {
                    onShowMessage("Gagal membaca file backup JSON.")
                }
            }
        }
    }

    val exportProductsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = writeTextToUri(context, uri, onGenerateProductsCsv())
                onShowMessage(
                    if (ok) "Ekspor CSV produk berhasil disimpan!" else "Gagal menulis file CSV produk."
                )
            }
        }
    }

    val exportTxCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = writeTextToUri(context, uri, onGenerateTransactionsCsv())
                onShowMessage(
                    if (ok) "Ekspor CSV transaksi berhasil disimpan!" else "Gagal menulis file CSV transaksi."
                )
            }
        }
    }

    val importProductsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = readTextFromUri(context, uri)
                if (text != null) {
                    onCsvProductsPicked(text)
                } else {
                    onShowMessage("Gagal membaca file CSV produk.")
                }
            }
        }
    }

    val report = uiState.dailyReport
    val todayStart = Formatters.startOfDayMillis(System.currentTimeMillis())
    val yesterdayStart = Formatters.addDays(todayStart, -1)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PosScreenHeader(
                title = "Laporan & Backup",
                subtitle = "Rekap harian & cadangan data lokal"
            )
        }

        // Date Selector Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onShiftReportDate(-1) },
                            modifier = Modifier.testTag("report_prev_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                contentDescription = "Hari sebelumnya"
                            )
                        }
                        TextButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.testTag("report_date_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Formatters.formatDateOnly(report.dateStartMillis),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        IconButton(
                            onClick = { onShiftReportDate(1) },
                            enabled = report.dateStartMillis < todayStart,
                            modifier = Modifier.testTag("report_next_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "Hari berikutnya"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        FilterChip(
                            selected = report.dateStartMillis == todayStart,
                            onClick = { onSelectReportDate(todayStart) },
                            label = { Text("Hari Ini") },
                            modifier = Modifier.testTag("report_today_chip")
                        )
                        FilterChip(
                            selected = report.dateStartMillis == yesterdayStart,
                            onClick = { onSelectReportDate(yesterdayStart) },
                            label = { Text("Kemarin") },
                            modifier = Modifier.testTag("report_yesterday_chip")
                        )
                    }
                }
            }
        }

        // Hero KPI Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Omzet Hero
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Omzet Penjualan Harian",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatRupiah(report.revenue),
                            style = MonoPriceStyle.copy(
                                fontSize = MaterialTheme.typography.displaySmall.fontSize
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Total HPP Modal: ${Formatters.formatRupiah(report.totalCost)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }

                // 2-Column KPIs: Transaksi + Laba Kotor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Transaksi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${report.transactionCount}",
                                style = MonoPriceStyle.copy(
                                    fontSize = MaterialTheme.typography.headlineMedium.fontSize
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${report.itemsSoldCount} item terjual",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Laba Kotor",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Formatters.formatRupiah(report.grossProfit),
                                style = MonoPriceStyle.copy(
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Margin ${report.profitMarginPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 7-Day Trend Chart
        item {
            SevenDayTrendCard(days = report.last7Days)
        }

        // Top Selling Products
        if (report.topProducts.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Produk Terlaris Hari Ini",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        report.topProducts.forEachIndexed { idx, stat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stat.productName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${stat.quantitySold} ${stat.unit} terjual",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = Formatters.formatRupiah(stat.totalRevenue),
                                    style = MonoPriceStyle.copy(
                                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (idx < report.topProducts.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        // Local Backup & Export/Import Section
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Backup,
                            contentDescription = "Backup Lokal",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Backup & Pindah Perangkat (Lokal)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simpan seluruh katalog produk dan riwayat transaksi ke file JSON atau CSV di penyimpanan HP agar aman saat ganti perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Backup Lengkap (JSON)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                exportJsonLauncher.launch("kasirku_backup_${Formatters.formatFileTimestamp()}.json")
                            },
                            enabled = !uiState.isBackupBusy,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("backup_export_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            enabled = !uiState.isBackupBusy,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("backup_import_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor JSON")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Ekspor & Impor Spreadsheet (CSV)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                exportProductsCsvLauncher.launch("kasirku_produk_${Formatters.formatFileTimestamp()}.csv")
                            },
                            enabled = !uiState.isBackupBusy,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("backup_export_products_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Produk CSV")
                        }
                        OutlinedButton(
                            onClick = {
                                importProductsCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                            },
                            enabled = !uiState.isBackupBusy,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("backup_import_products_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor CSV")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            exportTxCsvLauncher.launch("kasirku_transaksi_${Formatters.formatFileTimestamp()}.csv")
                        },
                        enabled = !uiState.isBackupBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("backup_export_transactions_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor Laporan Transaksi (CSV)")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = report.dateStartMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onSelectReportDate(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (uiState.pendingJsonImportContent != null) {
        AlertDialog(
            onDismissRequest = onDismissJsonImport,
            title = { Text("Pulihkan Backup JSON") },
            text = {
                Text(
                    "Pilih cara memulihkan data dari file backup:\n\n" +
                        "• Gabungkan: Menambah transaksi baru & memperbarui produk tanpa menghapus data lain.\n" +
                        "• Ganti Semua: Menghapus data saat ini dan menggantinya persis seperti file backup."
                )
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmJsonImport(BackupImportMode.MERGE) },
                    modifier = Modifier.testTag("confirm_import_merge_button")
                ) {
                    Text("Gabungkan")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onDismissJsonImport,
                        modifier = Modifier.testTag("cancel_import_json_button")
                    ) {
                        Text("Batal")
                    }
                    TextButton(
                        onClick = { onConfirmJsonImport(BackupImportMode.REPLACE) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("confirm_import_replace_button")
                    ) {
                        Text("Ganti Semua")
                    }
                }
            }
        )
    }
}

@Composable
private fun SevenDayTrendCard(days: List<DayBarStat>) {
    val maxRevenue = (days.maxOfOrNull { it.revenue } ?: 0L).coerceAtLeast(1L)
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val profitColor = MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tren Omzet 7 Hari",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LegendDot(color = primaryColor, label = "Omzet")
                    LegendDot(color = profitColor, label = "Laba")
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (days.isEmpty()) return@Canvas
                val slotWidth = size.width / days.size.toFloat()
                val barWidth = (slotWidth * 0.44f).coerceAtLeast(10f)
                val maxBarHeight = size.height

                days.forEachIndexed { index, stat ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val leftX = centerX - barWidth / 2f

                    // Track background
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(leftX, 0f),
                        size = Size(barWidth, maxBarHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    val revRatio = (stat.revenue.toFloat() / maxRevenue.toFloat()).coerceIn(0f, 1f)
                    val revHeight = (maxBarHeight * revRatio).coerceAtLeast(if (stat.revenue > 0) 8f else 0f)
                    if (revHeight > 0f) {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(leftX, maxBarHeight - revHeight),
                            size = Size(barWidth, revHeight),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }

                    val profitRatio = (stat.grossProfit.coerceAtLeast(0L).toFloat() / maxRevenue.toFloat()).coerceIn(0f, 1f)
                    val profitHeight = (maxBarHeight * profitRatio).coerceAtLeast(if (stat.grossProfit > 0) 6f else 0f)
                    if (profitHeight > 0f) {
                        drawRoundRect(
                            color = profitColor,
                            topLeft = Offset(leftX + barWidth * 0.2f, maxBarHeight - profitHeight),
                            size = Size(barWidth * 0.6f, profitHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { stat ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (stat.revenue > 0L) {
                            Text(
                                text = Formatters.formatShortRupiah(stat.revenue).removePrefix("Rp "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return@withContext false
            true
        } catch (_: Exception) {
            false
        }
    }

private suspend fun readTextFromUri(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (_: Exception) {
            null
        }
    }
