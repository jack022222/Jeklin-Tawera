package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionWithItems
import com.example.ui.HistoryFilter
import com.example.ui.PosUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PosScreenHeader
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.MonoReceiptStyle
import com.example.util.Formatters

@Composable
fun RiwayatScreen(
    uiState: PosUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    onOpenReceipt: (TransactionWithItems) -> Unit,
    onShareReceipt: (TransactionWithItems) -> Unit,
    onDismissDeleteTx: () -> Unit,
    onConfirmDeleteTx: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        PosScreenHeader(
            title = "Riwayat",
            subtitle = "${uiState.filteredTransactions.size} transaksi ditampilkan"
        )

        OutlinedTextField(
            value = uiState.historySearchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari no. struk atau nama produk...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari riwayat"
                )
            },
            trailingIcon = {
                if (uiState.historySearchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.testTag("riwayat_clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Bersihkan pencarian"
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("riwayat_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                HistoryFilter.TODAY to "Hari Ini",
                HistoryFilter.SEVEN_DAYS to "7 Hari Terakhir",
                HistoryFilter.ALL to "Semua"
            )
            items(filters, key = { it.first.name }) { (filter, label) ->
                FilterChip(
                    selected = uiState.historyFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("riwayat_filter_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.filteredTransactions.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Outlined.ReceiptLong,
                title = "Belum Ada Riwayat Transaksi",
                description = "Transaksi yang selesai di menu Kasir akan otomatis tersimpan di sini beserta rincian struknya."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.filteredTransactions,
                    key = { it.transaction.id }
                ) { txWithItems ->
                    TransactionHistoryCard(
                        txWithItems = txWithItems,
                        onOpen = { onOpenReceipt(txWithItems) },
                        onShare = { onShareReceipt(txWithItems) }
                    )
                }
            }
        }
    }

    uiState.transactionToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissDeleteTx,
            title = { Text("Hapus Riwayat Transaksi?") },
            text = {
                Text(
                    "Struk ${target.transaction.invoiceNumber} senilai ${Formatters.formatRupiah(target.transaction.totalAmount)} akan dihapus dari riwayat & laporan."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteTx,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_tx_button")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteTx,
                    modifier = Modifier.testTag("cancel_delete_tx_button")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun TransactionHistoryCard(
    txWithItems: TransactionWithItems,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val tx = txWithItems.transaction
    val totalQty = txWithItems.items.sumOf { it.quantity }
    val itemPreview = txWithItems.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("riwayat_card_${tx.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.invoiceNumber,
                        style = MonoReceiptStyle.copy(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Formatters.formatDateTime(tx.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("riwayat_share_button_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Bagikan struk ${tx.invoiceNumber}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = itemPreview.ifEmpty { "-" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$totalQty item",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "Kembali ${Formatters.formatRupiah(tx.changeAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = Formatters.formatRupiah(tx.totalAmount),
                    style = MonoPriceStyle.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
