package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.ui.CartLine
import com.example.ui.DiscountMode
import com.example.ui.PosUiState
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PosScreenHeader
import com.example.ui.theme.MonoPriceStyle
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(
    uiState: PosUiState,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onDecrementCartItem: (Long) -> Unit,
    onRemoveCartItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onOpenCartSheet: () -> Unit,
    onCloseCartSheet: () -> Unit,
    onDiscountModeChange: (DiscountMode) -> Unit,
    onDiscountInputChange: (String) -> Unit,
    onCashInputChange: (String) -> Unit,
    onExactCashClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmitCheckout: () -> Unit,
    onNavigateToProduk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartQtyMap = uiState.cartLines.associate { it.product.id to it.quantity }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PosScreenHeader(
                title = "Kasir",
                subtitle = "${uiState.allProducts.size} produk tersedia"
            )

            OutlinedTextField(
                value = uiState.kasirSearchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari nama produk atau kategori...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari produk"
                    )
                },
                trailingIcon = {
                    if (uiState.kasirSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.testTag("kasir_clear_search_button")
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
                    .testTag("kasir_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            CategoryFilterRow(
                categories = uiState.categories,
                selectedCategory = uiState.kasirSelectedCategory,
                onSelectCategory = onCategorySelect
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.allProducts.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Outlined.Inventory2,
                    title = "Belum Ada Produk di Katalog",
                    description = "Tambahkan produk terlebih dahulu di menu Produk agar siap dijual melalui kasir.",
                    actionLabel = "Tambah Produk Sekarang",
                    actionTestTag = "kasir_goto_produk_button",
                    onActionClick = onNavigateToProduk
                )
            } else if (uiState.kasirFilteredProducts.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Outlined.Search,
                    title = "Produk Tidak Ditemukan",
                    description = "Coba kata kunci lain atau pilih kategori \"Semua\"."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 158.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = if (uiState.cartItemCount > 0) 96.dp else 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.kasirFilteredProducts, key = { it.id }) { product ->
                        val qtyInCart = cartQtyMap[product.id] ?: 0
                        KasirProductCard(
                            product = product,
                            qtyInCart = qtyInCart,
                            onAdd = { onAddToCart(product) },
                            onDecrement = { onDecrementCartItem(product.id) }
                        )
                    }
                }
            }
        }

        // Sticky Floating Cart Bar
        AnimatedVisibility(
            visible = uiState.cartItemCount > 0,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCartSheet)
                    .testTag("kasir_cart_summary_bar"),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = "Keranjang",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column {
                            Text(
                                text = "${uiState.cartItemCount} item di keranjang",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = Formatters.formatRupiah(uiState.cartTotal),
                                style = MonoPriceStyle.copy(
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Bayar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isCartSheetOpen) {
        BackHandler { onCloseCartSheet() }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseCartSheet,
            sheetState = sheetState
        ) {
            CartCheckoutSheetContent(
                cartLines = uiState.cartLines,
                subtotal = uiState.cartSubtotal,
                discountMode = uiState.discountMode,
                discountInput = uiState.discountInput,
                calculatedDiscount = uiState.calculatedDiscount,
                totalAmount = uiState.cartTotal,
                cashInput = uiState.cashInput,
                cashAmount = uiState.cashAmount,
                changeAmount = uiState.changeAmount,
                checkoutNote = uiState.checkoutNote,
                isProcessing = uiState.isProcessingCheckout,
                onIncrement = onAddToCart,
                onDecrement = onDecrementCartItem,
                onRemove = onRemoveCartItem,
                onClearCart = onClearCart,
                onDiscountModeChange = onDiscountModeChange,
                onDiscountInputChange = onDiscountInputChange,
                onCashInputChange = onCashInputChange,
                onExactCashClick = onExactCashClick,
                onNoteChange = onNoteChange,
                onSubmitCheckout = onSubmitCheckout
            )
        }
    }
}

@Composable
private fun KasirProductCard(
    product: ProductEntity,
    qtyInCart: Int,
    onAdd: () -> Unit,
    onDecrement: () -> Unit
) {
    val outOfStock = product.stock <= 0
    val isLowStock = product.stock in 1..5
    val inCart = qtyInCart > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !outOfStock, onClick = onAdd)
            .testTag("kasir_product_card_${product.id}"),
        shape = MaterialTheme.shapes.medium,
        color = when {
            outOfStock -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            inCart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (inCart) 1.5.dp else 1.dp,
            color = if (inCart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = if (inCart) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (inCart) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = qtyInCart.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (outOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 38.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = Formatters.formatRupiah(product.sellingPrice),
                style = MonoPriceStyle.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                ),
                color = if (outOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (outOfStock) "Stok Habis" else "Stok: ${product.stock} ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        outOfStock -> MaterialTheme.colorScheme.error
                        isLowStock -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (inCart) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = onDecrement)
                                .testTag("kasir_card_minus_${product.id}"),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Kurangi ${product.name}",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(enabled = qtyInCart < product.stock, onClick = onAdd)
                                .testTag("kasir_card_plus_${product.id}"),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Tambah ${product.name}",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartCheckoutSheetContent(
    cartLines: List<CartLine>,
    subtotal: Long,
    discountMode: DiscountMode,
    discountInput: String,
    calculatedDiscount: Long,
    totalAmount: Long,
    cashInput: String,
    cashAmount: Long,
    changeAmount: Long,
    checkoutNote: String,
    isProcessing: Boolean,
    onIncrement: (ProductEntity) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClearCart: () -> Unit,
    onDiscountModeChange: (DiscountMode) -> Unit,
    onDiscountInputChange: (String) -> Unit,
    onCashInputChange: (String) -> Unit,
    onExactCashClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmitCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Keranjang & Pembayaran",
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(
                onClick = onClearCart,
                modifier = Modifier.testTag("checkout_clear_cart_button")
            ) {
                Text("Kosongkan", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Cart lines
        cartLines.forEach { line ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = line.product.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${Formatters.formatRupiah(line.product.sellingPrice)} / ${line.product.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onDecrement(line.product.id) },
                            modifier = Modifier.testTag("checkout_minus_${line.product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Kurangi jumlah"
                            )
                        }
                        Text(
                            text = line.quantity.toString(),
                            style = MonoPriceStyle.copy(
                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                            )
                        )
                        IconButton(
                            onClick = { onIncrement(line.product) },
                            enabled = line.quantity < line.product.stock,
                            modifier = Modifier.testTag("checkout_plus_${line.product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Tambah jumlah"
                            )
                        }
                        IconButton(
                            onClick = { onRemove(line.product.id) },
                            modifier = Modifier.testTag("checkout_remove_${line.product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Hapus dari keranjang",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Discount section
        Text(
            text = "Diskon Opsional",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = discountMode == DiscountMode.NOMINAL,
                onClick = { onDiscountModeChange(DiscountMode.NOMINAL) },
                label = { Text("Nominal (Rp)") },
                modifier = Modifier.testTag("discount_mode_nominal")
            )
            FilterChip(
                selected = discountMode == DiscountMode.PERCENT,
                onClick = { onDiscountModeChange(DiscountMode.PERCENT) },
                label = { Text("Persen (%)") },
                modifier = Modifier.testTag("discount_mode_percent")
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = discountInput,
            onValueChange = onDiscountInputChange,
            label = {
                Text(if (discountMode == DiscountMode.NOMINAL) "Diskon (Rp)" else "Diskon (%)")
            },
            placeholder = { Text("0") },
            prefix = if (discountMode == DiscountMode.NOMINAL) {
                { Text("Rp ") }
            } else null,
            suffix = if (discountMode == DiscountMode.PERCENT) {
                { Text("%") }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_discount_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Totals card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = Formatters.formatRupiah(subtotal),
                        style = MonoPriceStyle.copy(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                if (calculatedDiscount > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Diskon", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "-${Formatters.formatRupiah(calculatedDiscount)}",
                            style = MonoPriceStyle.copy(
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Bayar",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = Formatters.formatRupiah(totalAmount),
                        style = MonoPriceStyle.copy(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cash input + quick chips
        Text(
            text = "Pembayaran Tunai",
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AssistChip(
                    onClick = onExactCashClick,
                    label = { Text("Uang Pas") },
                    modifier = Modifier.testTag("quick_cash_exact")
                )
            }
            val presets = listOf(10_000L, 20_000L, 50_000L, 100_000L)
            items(presets) { amount ->
                AssistChip(
                    onClick = { onCashInputChange(amount.toString()) },
                    label = { Text(Formatters.formatShortRupiah(amount)) },
                    modifier = Modifier.testTag("quick_cash_$amount")
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = cashInput,
            onValueChange = onCashInputChange,
            label = { Text("Uang Tunai Diterima") },
            prefix = { Text("Rp ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_cash_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Kembalian indicator
        val isCashSufficient = cashAmount >= totalAmount
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = if (isCashSufficient) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCashSufficient) "Kembalian" else "Kurang Bayar",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCashSufficient) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Text(
                    text = Formatters.formatRupiah(kotlin.math.abs(changeAmount)),
                    style = MonoPriceStyle.copy(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    ),
                    color = if (isCashSufficient) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = checkoutNote,
            onValueChange = onNoteChange,
            label = { Text("Catatan (opsional)") },
            placeholder = { Text("Misal: Meja 2 / Bungkus") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_note_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmitCheckout,
            enabled = cartLines.isNotEmpty() && isCashSufficient && !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("checkout_submit_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.PointOfSale,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isProcessing) "Menyimpan..." else "Proses & Simpan Transaksi",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
