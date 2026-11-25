package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionWithItems
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.screens.KasirScreen
import com.example.ui.screens.LaporanBackupScreen
import com.example.ui.screens.ProdukScreen
import com.example.ui.screens.RiwayatScreen
import com.example.util.Formatters

private data class NavDestinationItem(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navItems = listOf(
    NavDestinationItem(
        tab = MainTab.KASIR,
        label = "Kasir",
        selectedIcon = Icons.Filled.PointOfSale,
        unselectedIcon = Icons.Outlined.PointOfSale,
        testTag = "nav_kasir"
    ),
    NavDestinationItem(
        tab = MainTab.PRODUK,
        label = "Produk",
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2,
        testTag = "nav_produk"
    ),
    NavDestinationItem(
        tab = MainTab.RIWAYAT,
        label = "Riwayat",
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_riwayat"
    ),
    NavDestinationItem(
        tab = MainTab.LAPORAN,
        label = "Laporan",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_laporan"
    )
)

@Composable
fun KasirKuApp(viewModel: PosViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val shareReceipt: (TransactionWithItems) -> Unit = { tx ->
        val receiptText = Formatters.buildShareableReceiptText(tx)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Struk ${tx.transaction.invoiceNumber}")
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        val chooser = Intent.createChooser(sendIntent, "Bagikan Struk Transaksi")
        context.startActivity(chooser)
    }

    val copyReceipt: (TransactionWithItems) -> Unit = { tx ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Struk ${tx.transaction.invoiceNumber}", Formatters.buildShareableReceiptText(tx))
        )
        viewModel.showMessage("Teks struk disalin ke clipboard.")
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.activeTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    NavTabIcon(
                                        item = item,
                                        selected = selected,
                                        cartCount = uiState.cartItemCount,
                                        lowStockCount = uiState.lowStockCount
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.activeTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    NavTabIcon(
                                        item = item,
                                        selected = selected,
                                        cartCount = uiState.cartItemCount,
                                        lowStockCount = uiState.lowStockCount
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                when (uiState.activeTab) {
                    MainTab.KASIR -> KasirScreen(
                        uiState = uiState,
                        onSearchChange = viewModel::setKasirSearch,
                        onCategorySelect = viewModel::setKasirCategory,
                        onAddToCart = viewModel::addToCart,
                        onDecrementCartItem = viewModel::decrementCartItem,
                        onRemoveCartItem = viewModel::removeFromCart,
                        onClearCart = viewModel::clearCart,
                        onOpenCartSheet = { viewModel.setCartSheetOpen(true) },
                        onCloseCartSheet = { viewModel.setCartSheetOpen(false) },
                        onDiscountModeChange = viewModel::setDiscountMode,
                        onDiscountInputChange = viewModel::setDiscountInput,
                        onCashInputChange = viewModel::setCashInput,
                        onExactCashClick = viewModel::setExactCash,
                        onNoteChange = viewModel::setCheckoutNote,
                        onSubmitCheckout = viewModel::submitCheckout,
                        onNavigateToProduk = {
                            viewModel.selectTab(MainTab.PRODUK)
                            viewModel.openAddProductForm()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    MainTab.PRODUK -> ProdukScreen(
                        uiState = uiState,
                        onSearchChange = viewModel::setProductSearch,
                        onCategorySelect = viewModel::setProductCategory,
                        onToggleLowStock = viewModel::toggleLowStockFilter,
                        onOpenAddForm = viewModel::openAddProductForm,
                        onOpenEditForm = viewModel::openEditProductForm,
                        onCloseForm = viewModel::closeProductForm,
                        onSaveProduct = viewModel::saveProduct,
                        onQuickAdjustStock = viewModel::quickAdjustStock,
                        onRequestDelete = viewModel::requestDeleteProduct,
                        onDismissDelete = viewModel::dismissDeleteProduct,
                        onConfirmDelete = viewModel::confirmDeleteProduct,
                        modifier = Modifier.weight(1f)
                    )

                    MainTab.RIWAYAT -> RiwayatScreen(
                        uiState = uiState,
                        onSearchChange = viewModel::setHistorySearch,
                        onFilterChange = viewModel::setHistoryFilter,
                        onOpenReceipt = viewModel::openReceipt,
                        onShareReceipt = shareReceipt,
                        onDismissDeleteTx = viewModel::dismissDeleteTransaction,
                        onConfirmDeleteTx = viewModel::confirmDeleteTransaction,
                        modifier = Modifier.weight(1f)
                    )

                    MainTab.LAPORAN -> LaporanBackupScreen(
                        uiState = uiState,
                        onShiftReportDate = viewModel::shiftReportDate,
                        onSelectReportDate = viewModel::setReportDate,
                        onGenerateJsonBackup = viewModel::generateJsonBackup,
                        onGenerateProductsCsv = viewModel::generateProductsCsv,
                        onGenerateTransactionsCsv = viewModel::generateTransactionsCsv,
                        onJsonFilePicked = { content -> viewModel.setPendingJsonImport(content) },
                        onDismissJsonImport = { viewModel.setPendingJsonImport(null) },
                        onConfirmJsonImport = viewModel::confirmJsonImport,
                        onCsvProductsPicked = viewModel::importProductsCsv,
                        onShowMessage = viewModel::showMessage,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    uiState.selectedReceipt?.let { receipt ->
        ReceiptDetailDialog(
            txWithItems = receipt,
            onDismiss = viewModel::closeReceipt,
            onShare = shareReceipt,
            onCopy = copyReceipt,
            onRequestDelete = viewModel::requestDeleteTransaction
        )
    }
}

@Composable
private fun NavTabIcon(
    item: NavDestinationItem,
    selected: Boolean,
    cartCount: Int,
    lowStockCount: Int
) {
    val icon = if (selected) item.selectedIcon else item.unselectedIcon
    val badgeCount = when (item.tab) {
        MainTab.KASIR -> cartCount
        MainTab.PRODUK -> lowStockCount
        else -> 0
    }

    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = if (item.tab == MainTab.PRODUK) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ) {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = item.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = item.label)
    }
}
