package com.example.data.repository

import com.example.data.local.CheckoutLineRequest
import com.example.data.local.PosDao
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionItemEntity
import com.example.data.local.TransactionWithItems
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class BackupImportMode {
    MERGE,
    REPLACE
}

data class BackupRestoreSummary(
    val productsCount: Int,
    val transactionsCount: Int
)

class PosRepository(private val posDao: PosDao) {

    val allProducts: Flow<List<ProductEntity>> = posDao.observeAllProducts()
    val allTransactions: Flow<List<TransactionWithItems>> = posDao.observeAllTransactionsWithItems()

    suspend fun insertProduct(product: ProductEntity): Long = posDao.insertProduct(product)

    suspend fun updateProduct(product: ProductEntity) = posDao.updateProduct(product)

    suspend fun deleteProduct(product: ProductEntity) = posDao.deleteProduct(product)

    suspend fun updateProductStock(productId: Long, newStock: Int) {
        posDao.updateStock(productId, newStock.coerceAtLeast(0), System.currentTimeMillis())
    }

    suspend fun completeSale(
        requests: List<CheckoutLineRequest>,
        discountAmount: Long,
        cashReceived: Long,
        note: String
    ): TransactionWithItems {
        val txId = posDao.completeSale(
            requests = requests,
            discountAmount = discountAmount,
            cashReceived = cashReceived,
            note = note
        )
        return posDao.getTransactionWithItemsById(txId)
            ?: throw IllegalStateException("Transaksi tersimpan tetapi gagal dimuat ulang.")
    }

    suspend fun deleteTransactionById(transactionId: Long) {
        posDao.deleteTransactionById(transactionId)
    }

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val products = posDao.getAllProductsOnce()
        val transactions = posDao.getAllTransactionsWithItemsOnce()

        val root = JSONObject().apply {
            put("app", "KasirKu")
            put("schemaVersion", 1)
            put("exportedAt", System.currentTimeMillis())

            val productsArray = JSONArray()
            for (p in products) {
                productsArray.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("category", p.category)
                        put("costPrice", p.costPrice)
                        put("sellingPrice", p.sellingPrice)
                        put("stock", p.stock)
                        put("unit", p.unit)
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                    }
                )
            }
            put("products", productsArray)

            val txArray = JSONArray()
            for (txWithItems in transactions) {
                val tx = txWithItems.transaction
                val itemsArray = JSONArray()
                for (item in txWithItems.items) {
                    itemsArray.put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("productId", item.productId ?: JSONObject.NULL)
                            put("productName", item.productName)
                            put("category", item.category)
                            put("unit", item.unit)
                            put("unitCostPrice", item.unitCostPrice)
                            put("unitSellingPrice", item.unitSellingPrice)
                            put("quantity", item.quantity)
                            put("lineTotal", item.lineTotal)
                        }
                    )
                }

                txArray.put(
                    JSONObject().apply {
                        put("id", tx.id)
                        put("invoiceNumber", tx.invoiceNumber)
                        put("timestamp", tx.timestamp)
                        put("subtotal", tx.subtotal)
                        put("discountAmount", tx.discountAmount)
                        put("totalAmount", tx.totalAmount)
                        put("totalCost", tx.totalCost)
                        put("cashReceived", tx.cashReceived)
                        put("changeAmount", tx.changeAmount)
                        put("note", tx.note)
                        put("items", itemsArray)
                    }
                )
            }
            put("transactions", txArray)
        }

        root.toString(2)
    }

    suspend fun importFullBackupJson(
        jsonString: String,
        mode: BackupImportMode
    ): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val productsArray = root.optJSONArray("products") ?: JSONArray()
        val txArray = root.optJSONArray("transactions") ?: JSONArray()

        val products = buildList {
            for (i in 0 until productsArray.length()) {
                val obj = productsArray.getJSONObject(i)
                val name = obj.optString("name").trim()
                if (name.isEmpty()) continue
                add(
                    ProductEntity(
                        id = obj.optLong("id", 0L),
                        name = name,
                        category = obj.optString("category", "Umum").ifBlank { "Umum" },
                        costPrice = obj.optLong("costPrice", 0L).coerceAtLeast(0L),
                        sellingPrice = obj.optLong("sellingPrice", 0L).coerceAtLeast(0L),
                        stock = obj.optInt("stock", 0).coerceAtLeast(0),
                        unit = obj.optString("unit", "pcs").ifBlank { "pcs" },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val transactions = buildList {
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val invoiceNumber = obj.optString("invoiceNumber").trim()
                if (invoiceNumber.isEmpty()) continue

                val txEntity = TransactionEntity(
                    id = obj.optLong("id", 0L),
                    invoiceNumber = invoiceNumber,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    subtotal = obj.optLong("subtotal", 0L),
                    discountAmount = obj.optLong("discountAmount", 0L),
                    totalAmount = obj.optLong("totalAmount", 0L),
                    totalCost = obj.optLong("totalCost", 0L),
                    cashReceived = obj.optLong("cashReceived", 0L),
                    changeAmount = obj.optLong("changeAmount", 0L),
                    note = obj.optString("note", "")
                )

                val itemsJson = obj.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (j in 0 until itemsJson.length()) {
                        val itemObj = itemsJson.getJSONObject(j)
                        val rawProductId = if (itemObj.isNull("productId")) null else itemObj.optLong("productId")
                        add(
                            TransactionItemEntity(
                                id = 0L,
                                transactionId = txEntity.id,
                                productId = rawProductId,
                                productName = itemObj.optString("productName", "Produk"),
                                category = itemObj.optString("category", "Umum"),
                                unit = itemObj.optString("unit", "pcs"),
                                unitCostPrice = itemObj.optLong("unitCostPrice", 0L),
                                unitSellingPrice = itemObj.optLong("unitSellingPrice", 0L),
                                quantity = itemObj.optInt("quantity", 1).coerceAtLeast(1),
                                lineTotal = itemObj.optLong("lineTotal", 0L)
                            )
                        )
                    }
                }
                add(TransactionWithItems(txEntity, items))
            }
        }

        when (mode) {
            BackupImportMode.REPLACE -> {
                posDao.replaceAllData(products, transactions)
                BackupRestoreSummary(products.size, transactions.size)
            }
            BackupImportMode.MERGE -> {
                val (pCount, tCount) = posDao.mergeBackupData(products, transactions)
                BackupRestoreSummary(pCount, tCount)
            }
        }
    }

    suspend fun exportProductsCsv(): String = withContext(Dispatchers.IO) {
        val products = posDao.getAllProductsOnce()
        val sb = StringBuilder()
        sb.appendLine("nama,kategori,harga_beli_hpp,harga_jual,stok,satuan")
        for (p in products) {
            sb.appendLine(
                listOf(
                    csvEscape(p.name),
                    csvEscape(p.category),
                    p.costPrice.toString(),
                    p.sellingPrice.toString(),
                    p.stock.toString(),
                    csvEscape(p.unit)
                ).joinToString(",")
            )
        }
        sb.toString()
    }

    suspend fun exportTransactionsCsv(): String = withContext(Dispatchers.IO) {
        val transactions = posDao.getAllTransactionsWithItemsOnce()
        val sb = StringBuilder()
        sb.appendLine("no_struk,tanggal,subtotal,diskon,total_bayar,total_hpp,laba_kotor,tunai,kembalian,catatan,daftar_item")
        for (txWithItems in transactions) {
            val tx = txWithItems.transaction
            val itemsSummary = txWithItems.items.joinToString("; ") {
                "${it.productName} (${it.quantity} ${it.unit} x ${it.unitSellingPrice})"
            }
            sb.appendLine(
                listOf(
                    csvEscape(tx.invoiceNumber),
                    csvEscape(Formatters.formatDateTime(tx.timestamp)),
                    tx.subtotal.toString(),
                    tx.discountAmount.toString(),
                    tx.totalAmount.toString(),
                    tx.totalCost.toString(),
                    tx.grossProfit.toString(),
                    tx.cashReceived.toString(),
                    tx.changeAmount.toString(),
                    csvEscape(tx.note),
                    csvEscape(itemsSummary)
                ).joinToString(",")
            )
        }
        sb.toString()
    }

    suspend fun importProductsCsv(csvText: String): Int = withContext(Dispatchers.IO) {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext 0

        var importedCount = 0
        val now = System.currentTimeMillis()
        for ((index, line) in lines.withIndex()) {
            val cols = parseCsvLine(line)
            if (cols.isEmpty()) continue
            if (index == 0 && cols[0].equals("nama", ignoreCase = true)) continue

            val name = cols.getOrNull(0)?.trim().orEmpty()
            if (name.isEmpty()) continue

            val category = cols.getOrNull(1)?.trim().orEmpty().ifBlank { "Umum" }
            val costPrice = cols.getOrNull(2)?.trim()?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val sellingPrice = cols.getOrNull(3)?.trim()?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val stock = cols.getOrNull(4)?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val unit = cols.getOrNull(5)?.trim().orEmpty().ifBlank { "pcs" }

            val existing = posDao.getProductByName(name)
            if (existing != null) {
                posDao.updateProduct(
                    existing.copy(
                        category = category,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        stock = stock,
                        unit = unit,
                        updatedAt = now
                    )
                )
            } else {
                posDao.insertProduct(
                    ProductEntity(
                        name = name,
                        category = category,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        stock = stock,
                        unit = unit,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            importedCount++
        }
        importedCount
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
