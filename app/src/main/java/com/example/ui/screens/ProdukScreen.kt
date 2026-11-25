package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ProductEntity
import com.example.ui.PosUiState
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PosScreenHeader
import com.example.ui.theme.MonoPriceStyle
import com.example.util.Formatters

@Composable
fun ProdukScreen(
    uiState: PosUiState,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onToggleLowStock: () -> Unit,
    onOpenAddForm: () -> Unit,
    onOpenEditForm: (ProductEntity) -> Unit,
    onCloseForm: () -> Unit,
    onSaveProduct: (
        id: Long?,
        name: String,
        category: String,
        costPrice: Long,
        sellingPrice: Long,
        stock: Int,
        unit: String
    ) -> Unit,
    onQuickAdjustStock: (ProductEntity, Int) -> Unit,
    onRequestDelete: (ProductEntity) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PosScreenHeader(
                title = "Produk & Stok",
                subtitle = "${uiState.allProducts.size} produk • ${uiState.lowStockCount} stok menipis"
            )

            OutlinedTextField(
                value = uiState.productSearchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari produk atau kategori...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari produk"
                    )
                },
                trailingIcon = {
                    if (uiState.productSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.testTag("produk_clear_search_button")
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
                    .testTag("produk_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            CategoryFilterRow(
                categories = uiState.categories,
                selectedCategory = uiState.productSelectedCategory,
                onSelectCategory = onCategorySelect,
                extraLeadingChip = {
                    FilterChip(
                        selected = uiState.onlyLowStock,
                        onClick = onToggleLowStock,
                        label = { Text("Stok <= 5 (${uiState.lowStockCount})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.testTag("produk_low_stock_chip")
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.allProducts.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Outlined.Inventory2,
                    title = "Katalog Produk Kosong",
                    description = "Mulai catat barang dagangan Anda lengkap dengan HPP, harga jual, stok, dan satuannya.",
                    actionLabel = "Tambah Produk Pertama",
                    actionTestTag = "produk_empty_add_button",
                    onActionClick = onOpenAddForm
                )
            } else if (uiState.productFilteredList.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Outlined.Search,
                    title = "Tidak Ada Produk Cocok",
                    description = "Ubah kata kunci pencarian atau filter kategori."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.productFilteredList, key = { it.id }) { product ->
                        ProductManagementCard(
                            product = product,
                            onEdit = { onOpenEditForm(product) },
                            onDelete = { onRequestDelete(product) },
                            onAdjustStock = { delta -> onQuickAdjustStock(product, delta) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenAddForm,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("produk_add_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Tambah Produk"
            )
        }
    }

    if (uiState.isProductFormOpen) {
        ProductFormDialog(
            editingProduct = uiState.editingProduct,
            existingCategories = uiState.categories,
            onDismiss = onCloseForm,
            onSave = onSaveProduct
        )
    }

    uiState.productToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Hapus Produk?") },
            text = {
                Text("Produk \"${target.name}\" akan dihapus dari katalog. Riwayat transaksi sebelumnya tetap aman.")
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_product_button")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDelete,
                    modifier = Modifier.testTag("cancel_delete_product_button")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ProductManagementCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Int) -> Unit
) {
    val isOut = product.stock <= 0
    val isLow = product.stock in 1..5

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("produk_item_card_${product.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (isOut || isLow) {
                            Surface(
                                color = if (isOut) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isOut) "Habis" else "Menipis",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOut) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("produk_edit_button_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit ${product.name}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("produk_delete_button_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Hapus ${product.name}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Harga Jual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatRupiah(product.sellingPrice),
                        style = MonoPriceStyle.copy(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "HPP: ${Formatters.formatRupiah(product.costPrice)} • Laba ${Formatters.formatRupiah(product.estimatedProfitPerUnit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = { onAdjustStock(-1) },
                            enabled = product.stock > 0,
                            modifier = Modifier.testTag("produk_stock_minus_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Kurangi stok",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "${product.stock}",
                                style = MonoPriceStyle.copy(
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                                ),
                                color = if (isOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = product.unit,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onAdjustStock(1) },
                            modifier = Modifier.testTag("produk_stock_plus_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Tambah stok",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductFormDialog(
    editingProduct: ProductEntity?,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long?,
        name: String,
        category: String,
        costPrice: Long,
        sellingPrice: Long,
        stock: Int,
        unit: String
    ) -> Unit
) {
    BackHandler { onDismiss() }

    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name ?: "") }
    var category by remember(editingProduct) { mutableStateOf(editingProduct?.category ?: "") }
    var costPriceText by remember(editingProduct) {
        mutableStateOf(editingProduct?.costPrice?.toString() ?: "")
    }
    var sellingPriceText by remember(editingProduct) {
        mutableStateOf(editingProduct?.sellingPrice?.toString() ?: "")
    }
    var stockText by remember(editingProduct) {
        mutableStateOf(editingProduct?.stock?.toString() ?: "")
    }
    var unit by remember(editingProduct) { mutableStateOf(editingProduct?.unit ?: "pcs") }
    var showValidationError by remember { mutableStateOf(false) }

    val suggestedCategories = remember(existingCategories) {
        (existingCategories + listOf("Makanan", "Minuman", "Sembako", "Snack", "ATK", "Lainnya"))
            .distinct()
            .take(8)
    }
    val suggestedUnits = remember {
        listOf("pcs", "bungkus", "botol", "porsi", "kg", "liter", "dus")
    }

    val costVal = costPriceText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val sellVal = sellingPriceText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val stockVal = stockText.filter { it.isDigit() }.toIntOrNull() ?: 0
    val marginProfit = sellVal - costVal
    val marginPct = if (sellVal > 0L) ((marginProfit.toDouble() / sellVal.toDouble()) * 100).toInt() else 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 540.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = if (editingProduct == null) "Tambah Produk Baru" else "Edit Produk",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showValidationError = false
                    },
                    label = { Text("Nama Produk *") },
                    placeholder = { Text("Contoh: Kopi Susu Gula Aren") },
                    isError = showValidationError && name.trim().isEmpty(),
                    supportingText = if (showValidationError && name.trim().isEmpty()) {
                        { Text("Nama produk wajib diisi") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_form_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    placeholder = { Text("Contoh: Minuman") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_form_category_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestedCategories) { cat ->
                        AssistChip(
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Beli / HPP") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_form_cost_input")
                    )
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Jual *") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_form_price_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live margin preview
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (marginProfit >= 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimasi Laba per Satuan",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${Formatters.formatRupiah(marginProfit)} ($marginPct%)",
                            style = MonoPriceStyle.copy(
                                fontSize = MaterialTheme.typography.bodySmall.fontSize
                            ),
                            color = if (marginProfit >= 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Jumlah Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_form_stock_input")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan") },
                        placeholder = { Text("pcs") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_form_unit_input")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestedUnits) { u ->
                        AssistChip(
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("product_form_cancel_button")
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                showValidationError = true
                            } else {
                                onSave(
                                    editingProduct?.id,
                                    name,
                                    category,
                                    costVal,
                                    sellVal,
                                    stockVal,
                                    unit
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("product_form_save_button")
                    ) {
                        Text(if (editingProduct == null) "Simpan Produk" else "Simpan Perubahan")
                    }
                }
            }
        }
    }
}
