package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CheckoutLineRequest
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionWithItems
import com.example.data.repository.BackupImportMode
import com.example.data.repository.PosRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
    KASIR,
    PRODUK,
    RIWAYAT,
    LAPORAN
}

enum class DiscountMode {
    NOMINAL,
    PERCENT
}

enum class HistoryFilter {
    TODAY,
    SEVEN_DAYS,
    ALL
}

data class CartLine(
    val product: ProductEntity,
    val quantity: Int
) {
    val lineTotal: Long get() = product.sellingPrice * quantity
    val lineCost: Long get() = product.costPrice * quantity
}

data class TopProductStat(
    val productName: String,
    val unit: String,
    val quantitySold: Int,
    val totalRevenue: Long
)

data class DayBarStat(
    val dayStartMillis: Long,
    val label: String,
    val revenue: Long,
    val grossProfit: Long,
    val transactionCount: Int
)

data class DailyReportSummary(
    val dateStartMillis: Long,
    val revenue: Long,
    val transactionCount: Int,
    val itemsSoldCount: Int,
    val totalCost: Long,
    val grossProfit: Long,
    val profitMarginPercent: Int,
    val topProducts: List<TopProductStat>,
    val last7Days: List<DayBarStat>
)

data class PosInternalState(
    val activeTab: MainTab = MainTab.KASIR,
    val kasirSearchQuery: String = "",
    val kasirSelectedCategory: String = "Semua",
    val cartQuantities: Map<Long, Int> = emptyMap(),
    val discountMode: DiscountMode = DiscountMode.NOMINAL,
    val discountInput: String = "",
    val cashInput: String = "",
    val checkoutNote: String = "",
    val isCartSheetOpen: Boolean = false,
    val isProcessingCheckout: Boolean = false,
    val productSearchQuery: String = "",
    val productSelectedCategory: String = "Semua",
    val onlyLowStock: Boolean = false,
    val isProductFormOpen: Boolean = false,
    val editingProduct: ProductEntity? = null,
    val productToDelete: ProductEntity? = null,
    val historySearchQuery: String = "",
    val historyFilter: HistoryFilter = HistoryFilter.TODAY,
    val selectedReceipt: TransactionWithItems? = null,
    val transactionToDelete: TransactionWithItems? = null,
    val reportDateStartMillis: Long = Formatters.startOfDayMillis(System.currentTimeMillis()),
    val pendingJsonImportContent: String? = null,
    val isBackupBusy: Boolean = false,
    val snackbarMessage: String? = null
)

data class PosUiState(
    val activeTab: MainTab = MainTab.KASIR,
    val allProducts: List<ProductEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val kasirFilteredProducts: List<ProductEntity> = emptyList(),
    val kasirSearchQuery: String = "",
    val kasirSelectedCategory: String = "Semua",
    val cartLines: List<CartLine> = emptyList(),
    val cartItemCount: Int = 0,
    val cartSubtotal: Long = 0L,
    val discountMode: DiscountMode = DiscountMode.NOMINAL,
    val discountInput: String = "",
    val calculatedDiscount: Long = 0L,
    val cartTotal: Long = 0L,
    val cashInput: String = "",
    val cashAmount: Long = 0L,
    val changeAmount: Long = 0L,
    val checkoutNote: String = "",
    val isCartSheetOpen: Boolean = false,
    val isProcessingCheckout: Boolean = false,
    val productFilteredList: List<ProductEntity> = emptyList(),
    val productSearchQuery: String = "",
    val productSelectedCategory: String = "Semua",
    val onlyLowStock: Boolean = false,
    val lowStockCount: Int = 0,
    val isProductFormOpen: Boolean = false,
    val editingProduct: ProductEntity? = null,
    val productToDelete: ProductEntity? = null,
    val allTransactions: List<TransactionWithItems> = emptyList(),
    val filteredTransactions: List<TransactionWithItems> = emptyList(),
    val historySearchQuery: String = "",
    val historyFilter: HistoryFilter = HistoryFilter.TODAY,
    val selectedReceipt: TransactionWithItems? = null,
    val transactionToDelete: TransactionWithItems? = null,
    val dailyReport: DailyReportSummary = DailyReportSummary(
        dateStartMillis = Formatters.startOfDayMillis(System.currentTimeMillis()),
        revenue = 0L,
        transactionCount = 0,
        itemsSoldCount = 0,
        totalCost = 0L,
        grossProfit = 0L,
        profitMarginPercent = 0,
        topProducts = emptyList(),
        last7Days = emptyList()
    ),
    val pendingJsonImportContent: String? = null,
    val isBackupBusy: Boolean = false,
    val snackbarMessage: String? = null
)

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    private val internalState = MutableStateFlow(PosInternalState())

    val uiState: StateFlow<PosUiState> = combine(
        repository.allProducts,
        repository.allTransactions,
        internalState
    ) { products, transactions, state ->
        val categories = products
            .map { it.category.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        val productById = products.associateBy { it.id }

        // Build cart lines clamped to available product stock
        val cartLines = state.cartQuantities.entries.mapNotNull { (productId, qty) ->
            val product = productById[productId] ?: return@mapNotNull null
            if (product.stock <= 0 || qty <= 0) return@mapNotNull null
            CartLine(product = product, quantity = qty.coerceAtMost(product.stock))
        }
        val cartItemCount = cartLines.sumOf { it.quantity }
        val cartSubtotal = cartLines.sumOf { it.lineTotal }

        val rawDiscount = state.discountInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val calculatedDiscount = when (state.discountMode) {
            DiscountMode.NOMINAL -> rawDiscount.coerceIn(0L, cartSubtotal)
            DiscountMode.PERCENT -> {
                val pct = rawDiscount.coerceIn(0L, 100L)
                ((cartSubtotal * pct) / 100L).coerceIn(0L, cartSubtotal)
            }
        }
        val cartTotal = (cartSubtotal - calculatedDiscount).coerceAtLeast(0L)
        val cashAmount = state.cashInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val changeAmount = cashAmount - cartTotal

        // Filter products for Kasir
        val kasirQuery = state.kasirSearchQuery.trim()
        val kasirFiltered = products.filter { p ->
            val matchesCategory = state.kasirSelectedCategory == "Semua" ||
                p.category.equals(state.kasirSelectedCategory, ignoreCase = true)
            val matchesQuery = kasirQuery.isEmpty() ||
                p.name.contains(kasirQuery, ignoreCase = true) ||
                p.category.contains(kasirQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        // Filter products for Produk screen
        val prodQuery = state.productSearchQuery.trim()
        val productFiltered = products.filter { p ->
            val matchesCategory = state.productSelectedCategory == "Semua" ||
                p.category.equals(state.productSelectedCategory, ignoreCase = true)
            val matchesLowStock = !state.onlyLowStock || p.stock <= 5
            val matchesQuery = prodQuery.isEmpty() ||
                p.name.contains(prodQuery, ignoreCase = true) ||
                p.category.contains(prodQuery, ignoreCase = true)
            matchesCategory && matchesLowStock && matchesQuery
        }

        // Filter transactions for Riwayat
        val todayStart = Formatters.startOfDayMillis(System.currentTimeMillis())
        val sevenDaysAgoStart = Formatters.addDays(todayStart, -6)
        val histQuery = state.historySearchQuery.trim()
        val filteredTx = transactions.filter { txWithItems ->
            val tx = txWithItems.transaction
            val matchesDate = when (state.historyFilter) {
                HistoryFilter.TODAY -> tx.timestamp >= todayStart
                HistoryFilter.SEVEN_DAYS -> tx.timestamp >= sevenDaysAgoStart
                HistoryFilter.ALL -> true
            }
            val matchesQuery = histQuery.isEmpty() ||
                tx.invoiceNumber.contains(histQuery, ignoreCase = true) ||
                tx.note.contains(histQuery, ignoreCase = true) ||
                txWithItems.items.any { it.productName.contains(histQuery, ignoreCase = true) }
            matchesDate && matchesQuery
        }

        // Build Daily Report Summary
        val reportStart = state.reportDateStartMillis
        val reportEnd = Formatters.addDays(reportStart, 1)
        val dayTx = transactions.filter {
            it.transaction.timestamp in reportStart until reportEnd
        }
        val dayRevenue = dayTx.sumOf { it.transaction.totalAmount }
        val dayCost = dayTx.sumOf { it.transaction.totalCost }
        val dayProfit = dayRevenue - dayCost
        val dayItemsSold = dayTx.sumOf { txWithItems -> txWithItems.items.sumOf { it.quantity } }
        val profitMarginPercent = if (dayRevenue > 0L) {
            ((dayProfit.toDouble() / dayRevenue.toDouble()) * 100.0).toInt()
        } else {
            0
        }

        val topProducts = dayTx
            .flatMap { it.items }
            .groupBy { it.productName to it.unit }
            .map { (key, items) ->
                TopProductStat(
                    productName = key.first,
                    unit = key.second,
                    quantitySold = items.sumOf { it.quantity },
                    totalRevenue = items.sumOf { it.lineTotal }
                )
            }
            .sortedByDescending { it.quantitySold }
            .take(5)

        val last7Days = (-6..0).map { offset ->
            val dStart = Formatters.addDays(reportStart, offset)
            val dEnd = Formatters.addDays(dStart, 1)
            val dTx = transactions.filter { it.transaction.timestamp in dStart until dEnd }
            val dRev = dTx.sumOf { it.transaction.totalAmount }
            val dCost = dTx.sumOf { it.transaction.totalCost }
            DayBarStat(
                dayStartMillis = dStart,
                label = Formatters.formatShortDate(dStart),
                revenue = dRev,
                grossProfit = dRev - dCost,
                transactionCount = dTx.size
            )
        }

        PosUiState(
            activeTab = state.activeTab,
            allProducts = products,
            categories = categories,
            kasirFilteredProducts = kasirFiltered,
            kasirSearchQuery = state.kasirSearchQuery,
            kasirSelectedCategory = state.kasirSelectedCategory,
            cartLines = cartLines,
            cartItemCount = cartItemCount,
            cartSubtotal = cartSubtotal,
            discountMode = state.discountMode,
            discountInput = state.discountInput,
            calculatedDiscount = calculatedDiscount,
            cartTotal = cartTotal,
            cashInput = state.cashInput,
            cashAmount = cashAmount,
            changeAmount = changeAmount,
            checkoutNote = state.checkoutNote,
            isCartSheetOpen = state.isCartSheetOpen,
            isProcessingCheckout = state.isProcessingCheckout,
            productFilteredList = productFiltered,
            productSearchQuery = state.productSearchQuery,
            productSelectedCategory = state.productSelectedCategory,
            onlyLowStock = state.onlyLowStock,
            lowStockCount = products.count { it.stock <= 5 },
            isProductFormOpen = state.isProductFormOpen,
            editingProduct = state.editingProduct,
            productToDelete = state.productToDelete,
            allTransactions = transactions,
            filteredTransactions = filteredTx,
            historySearchQuery = state.historySearchQuery,
            historyFilter = state.historyFilter,
            selectedReceipt = state.selectedReceipt,
            transactionToDelete = state.transactionToDelete,
            dailyReport = DailyReportSummary(
                dateStartMillis = reportStart,
                revenue = dayRevenue,
                transactionCount = dayTx.size,
                itemsSoldCount = dayItemsSold,
                totalCost = dayCost,
                grossProfit = dayProfit,
                profitMarginPercent = profitMarginPercent,
                topProducts = topProducts,
                last7Days = last7Days
            ),
            pendingJsonImportContent = state.pendingJsonImportContent,
            isBackupBusy = state.isBackupBusy,
            snackbarMessage = state.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PosUiState()
    )

    fun selectTab(tab: MainTab) {
        internalState.update { it.copy(activeTab = tab) }
    }

    fun consumeSnackbar() {
        internalState.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(message: String) {
        internalState.update { it.copy(snackbarMessage = message) }
    }

    // --- Kasir Actions ---
    fun setKasirSearch(query: String) {
        internalState.update { it.copy(kasirSearchQuery = query) }
    }

    fun setKasirCategory(category: String) {
        internalState.update { it.copy(kasirSelectedCategory = category) }
    }

    fun addToCart(product: ProductEntity) {
        if (product.stock <= 0) {
            showMessage("Stok '${product.name}' sedang habis.")
            return
        }
        internalState.update { state ->
            val currentQty = state.cartQuantities[product.id] ?: 0
            if (currentQty >= product.stock) {
                state.copy(snackbarMessage = "Maksimal stok '${product.name}' (${product.stock} ${product.unit}) sudah di keranjang.")
            } else {
                val updated = state.cartQuantities.toMutableMap()
                updated[product.id] = currentQty + 1
                state.copy(cartQuantities = updated)
            }
        }
    }

    fun incrementCartItem(product: ProductEntity) {
        addToCart(product)
    }

    fun decrementCartItem(productId: Long) {
        internalState.update { state ->
            val currentQty = state.cartQuantities[productId] ?: 0
            val updated = state.cartQuantities.toMutableMap()
            if (currentQty <= 1) {
                updated.remove(productId)
            } else {
                updated[productId] = currentQty - 1
            }
            val shouldClose = updated.isEmpty() && state.isCartSheetOpen
            state.copy(
                cartQuantities = updated,
                isCartSheetOpen = if (shouldClose) false else state.isCartSheetOpen
            )
        }
    }

    fun removeFromCart(productId: Long) {
        internalState.update { state ->
            val updated = state.cartQuantities.toMutableMap()
            updated.remove(productId)
            state.copy(
                cartQuantities = updated,
                isCartSheetOpen = if (updated.isEmpty()) false else state.isCartSheetOpen
            )
        }
    }

    fun clearCart() {
        internalState.update {
            it.copy(
                cartQuantities = emptyMap(),
                discountInput = "",
                cashInput = "",
                checkoutNote = "",
                isCartSheetOpen = false
            )
        }
    }

    fun setCartSheetOpen(open: Boolean) {
        internalState.update { it.copy(isCartSheetOpen = open) }
    }

    fun setDiscountMode(mode: DiscountMode) {
        internalState.update { it.copy(discountMode = mode, discountInput = "") }
    }

    fun setDiscountInput(input: String) {
        val digitsOnly = input.filter { it.isDigit() }
        internalState.update { it.copy(discountInput = digitsOnly) }
    }

    fun setCashInput(input: String) {
        val digitsOnly = input.filter { it.isDigit() }
        internalState.update { it.copy(cashInput = digitsOnly) }
    }

    fun setExactCash() {
        val total = uiState.value.cartTotal
        internalState.update { it.copy(cashInput = total.toString()) }
    }

    fun setCheckoutNote(note: String) {
        internalState.update { it.copy(checkoutNote = note) }
    }

    fun submitCheckout() {
        val snapshot = uiState.value
        if (snapshot.cartLines.isEmpty()) {
            showMessage("Keranjang masih kosong.")
            return
        }
        if (snapshot.cashAmount < snapshot.cartTotal) {
            showMessage("Uang tunai kurang ${Formatters.formatRupiah(snapshot.cartTotal - snapshot.cashAmount)}.")
            return
        }
        if (snapshot.isProcessingCheckout) return

        viewModelScope.launch {
            internalState.update { it.copy(isProcessingCheckout = true) }
            try {
                val requests = snapshot.cartLines.map { line ->
                    CheckoutLineRequest(
                        productId = line.product.id,
                        productName = line.product.name,
                        quantity = line.quantity
                    )
                }
                val savedTx = repository.completeSale(
                    requests = requests,
                    discountAmount = snapshot.calculatedDiscount,
                    cashReceived = snapshot.cashAmount,
                    note = snapshot.checkoutNote
                )
                internalState.update {
                    it.copy(
                        cartQuantities = emptyMap(),
                        discountInput = "",
                        cashInput = "",
                        checkoutNote = "",
                        isCartSheetOpen = false,
                        isProcessingCheckout = false,
                        selectedReceipt = savedTx,
                        snackbarMessage = "Transaksi ${savedTx.transaction.invoiceNumber} berhasil disimpan!"
                    )
                }
            } catch (e: Exception) {
                internalState.update {
                    it.copy(
                        isProcessingCheckout = false,
                        snackbarMessage = e.message ?: "Gagal menyimpan transaksi."
                    )
                }
            }
        }
    }

    // --- Product CRUD Actions ---
    fun setProductSearch(query: String) {
        internalState.update { it.copy(productSearchQuery = query) }
    }

    fun setProductCategory(category: String) {
        internalState.update { it.copy(productSelectedCategory = category) }
    }

    fun toggleLowStockFilter() {
        internalState.update { it.copy(onlyLowStock = !it.onlyLowStock) }
    }

    fun openAddProductForm() {
        internalState.update {
            it.copy(isProductFormOpen = true, editingProduct = null)
        }
    }

    fun openEditProductForm(product: ProductEntity) {
        internalState.update {
            it.copy(isProductFormOpen = true, editingProduct = product)
        }
    }

    fun closeProductForm() {
        internalState.update {
            it.copy(isProductFormOpen = false, editingProduct = null)
        }
    }

    fun saveProduct(
        id: Long?,
        name: String,
        category: String,
        costPrice: Long,
        sellingPrice: Long,
        stock: Int,
        unit: String
    ) {
        val cleanName = name.trim()
        val cleanCategory = category.trim().ifBlank { "Umum" }
        val cleanUnit = unit.trim().ifBlank { "pcs" }
        if (cleanName.isEmpty()) {
            showMessage("Nama produk wajib diisi.")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (id == null || id == 0L) {
                repository.insertProduct(
                    ProductEntity(
                        name = cleanName,
                        category = cleanCategory,
                        costPrice = costPrice.coerceAtLeast(0L),
                        sellingPrice = sellingPrice.coerceAtLeast(0L),
                        stock = stock.coerceAtLeast(0),
                        unit = cleanUnit,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                internalState.update {
                    it.copy(
                        isProductFormOpen = false,
                        editingProduct = null,
                        snackbarMessage = "Produk '$cleanName' berhasil ditambahkan."
                    )
                }
            } else {
                val existing = internalState.value.editingProduct
                repository.updateProduct(
                    ProductEntity(
                        id = id,
                        name = cleanName,
                        category = cleanCategory,
                        costPrice = costPrice.coerceAtLeast(0L),
                        sellingPrice = sellingPrice.coerceAtLeast(0L),
                        stock = stock.coerceAtLeast(0),
                        unit = cleanUnit,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                internalState.update {
                    it.copy(
                        isProductFormOpen = false,
                        editingProduct = null,
                        snackbarMessage = "Produk '$cleanName' berhasil diperbarui."
                    )
                }
            }
        }
    }

    fun quickAdjustStock(product: ProductEntity, delta: Int) {
        val nextStock = (product.stock + delta).coerceAtLeast(0)
        viewModelScope.launch {
            repository.updateProductStock(product.id, nextStock)
        }
    }

    fun requestDeleteProduct(product: ProductEntity) {
        internalState.update { it.copy(productToDelete = product) }
    }

    fun dismissDeleteProduct() {
        internalState.update { it.copy(productToDelete = null) }
    }

    fun confirmDeleteProduct() {
        val product = internalState.value.productToDelete ?: return
        viewModelScope.launch {
            repository.deleteProduct(product)
            internalState.update { state ->
                val updatedCart = state.cartQuantities.toMutableMap().apply { remove(product.id) }
                state.copy(
                    productToDelete = null,
                    cartQuantities = updatedCart,
                    snackbarMessage = "Produk '${product.name}' telah dihapus."
                )
            }
        }
    }

    // --- History & Receipt Actions ---
    fun setHistorySearch(query: String) {
        internalState.update { it.copy(historySearchQuery = query) }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        internalState.update { it.copy(historyFilter = filter) }
    }

    fun openReceipt(transactionWithItems: TransactionWithItems) {
        internalState.update { it.copy(selectedReceipt = transactionWithItems) }
    }

    fun closeReceipt() {
        internalState.update { it.copy(selectedReceipt = null) }
    }

    fun requestDeleteTransaction(transactionWithItems: TransactionWithItems) {
        internalState.update { it.copy(transactionToDelete = transactionWithItems) }
    }

    fun dismissDeleteTransaction() {
        internalState.update { it.copy(transactionToDelete = null) }
    }

    fun confirmDeleteTransaction() {
        val target = internalState.value.transactionToDelete ?: return
        viewModelScope.launch {
            repository.deleteTransactionById(target.transaction.id)
            internalState.update {
                it.copy(
                    transactionToDelete = null,
                    selectedReceipt = if (it.selectedReceipt?.transaction?.id == target.transaction.id) null else it.selectedReceipt,
                    snackbarMessage = "Riwayat ${target.transaction.invoiceNumber} dihapus."
                )
            }
        }
    }

    // --- Report & Backup Actions ---
    fun setReportDate(dateMillis: Long) {
        internalState.update {
            it.copy(reportDateStartMillis = Formatters.startOfDayMillis(dateMillis))
        }
    }

    fun shiftReportDate(days: Int) {
        internalState.update {
            it.copy(reportDateStartMillis = Formatters.addDays(it.reportDateStartMillis, days))
        }
    }

    suspend fun generateJsonBackup(): String = repository.exportFullBackupJson()

    suspend fun generateProductsCsv(): String = repository.exportProductsCsv()

    suspend fun generateTransactionsCsv(): String = repository.exportTransactionsCsv()

    fun setPendingJsonImport(jsonContent: String?) {
        internalState.update { it.copy(pendingJsonImportContent = jsonContent) }
    }

    fun confirmJsonImport(mode: BackupImportMode) {
        val rawJson = internalState.value.pendingJsonImportContent ?: return
        viewModelScope.launch {
            internalState.update { it.copy(isBackupBusy = true) }
            try {
                val summary = repository.importFullBackupJson(rawJson, mode)
                internalState.update {
                    it.copy(
                        pendingJsonImportContent = null,
                        isBackupBusy = false,
                        snackbarMessage = "Berhasil memulihkan ${summary.productsCount} produk & ${summary.transactionsCount} transaksi."
                    )
                }
            } catch (e: Exception) {
                internalState.update {
                    it.copy(
                        pendingJsonImportContent = null,
                        isBackupBusy = false,
                        snackbarMessage = "File backup JSON tidak valid: ${e.message ?: "format salah"}"
                    )
                }
            }
        }
    }

    fun importProductsCsv(csvText: String) {
        viewModelScope.launch {
            internalState.update { it.copy(isBackupBusy = true) }
            try {
                val count = repository.importProductsCsv(csvText)
                internalState.update {
                    it.copy(
                        isBackupBusy = false,
                        snackbarMessage = if (count > 0) {
                            "Berhasil mengimpor $count produk dari CSV."
                        } else {
                            "Tidak ada baris produk yang valid di file CSV."
                        }
                    )
                }
            } catch (e: Exception) {
                internalState.update {
                    it.copy(
                        isBackupBusy = false,
                        snackbarMessage = "Gagal impor CSV: ${e.message ?: "format tidak sesuai"}"
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: PosRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PosViewModel(repository) as T
                }
            }
        }
    }
}
