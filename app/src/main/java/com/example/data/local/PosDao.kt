package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CheckoutLineRequest(
    val productId: Long,
    val productName: String,
    val quantity: Int
)

@Dao
interface PosDao {

    // --- Products CRUD ---
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getProductByName(name: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)

    @Query(
        "UPDATE products SET stock = stock - :qty, updatedAt = :updatedAt " +
            "WHERE id = :productId AND stock >= :qty"
    )
    suspend fun decrementStock(productId: Long, qty: Int, updatedAt: Long): Int

    @Query("UPDATE products SET stock = :newStock, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int, updatedAt: Long)

    // --- Transactions ---
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsWithItemsOnce(): List<TransactionWithItems>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionWithItemsById(transactionId: Long): TransactionWithItems?

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    suspend fun countTransactionsInRange(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE invoiceNumber = :invoiceNumber)")
    suspend fun invoiceExists(invoiceNumber: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllTransactionItems()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Transaction
    suspend fun completeSale(
        requests: List<CheckoutLineRequest>,
        discountAmount: Long,
        cashReceived: Long,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        require(requests.isNotEmpty()) { "Keranjang belanja masih kosong." }

        val resolvedItems = mutableListOf<TransactionItemEntity>()
        var subtotal = 0L
        var totalCost = 0L

        for (req in requests) {
            require(req.quantity > 0) { "Jumlah item harus lebih dari 0." }
            val product = getProductById(req.productId)
                ?: throw IllegalStateException("Produk '${req.productName}' tidak ditemukan.")

            if (product.stock < req.quantity) {
                throw IllegalStateException(
                    "Stok '${product.name}' tidak mencukupi (tersisa ${product.stock} ${product.unit})."
                )
            }

            val updatedRows = decrementStock(product.id, req.quantity, timestamp)
            if (updatedRows == 0) {
                throw IllegalStateException(
                    "Gagal mengurangi stok '${product.name}'. Periksa stok terbaru."
                )
            }

            val lineTotal = product.sellingPrice * req.quantity
            val lineCost = product.costPrice * req.quantity
            subtotal += lineTotal
            totalCost += lineCost

            resolvedItems.add(
                TransactionItemEntity(
                    transactionId = 0L,
                    productId = product.id,
                    productName = product.name,
                    category = product.category,
                    unit = product.unit,
                    unitCostPrice = product.costPrice,
                    unitSellingPrice = product.sellingPrice,
                    quantity = req.quantity,
                    lineTotal = lineTotal
                )
            )
        }

        val clampedDiscount = discountAmount.coerceIn(0L, subtotal)
        val totalAmount = (subtotal - clampedDiscount).coerceAtLeast(0L)
        if (cashReceived < totalAmount) {
            throw IllegalStateException("Uang tunai belum mencukupi total pembayaran.")
        }
        val changeAmount = cashReceived - totalAmount

        val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
        val timePart = SimpleDateFormat("HHmmss", Locale.US).format(Date(timestamp))
        val suffix = (timestamp % 1000L).toString().padStart(3, '0')
        val invoiceNumber = "TRX-$datePart-$timePart-$suffix"

        val transactionId = insertTransaction(
            TransactionEntity(
                invoiceNumber = invoiceNumber,
                timestamp = timestamp,
                subtotal = subtotal,
                discountAmount = clampedDiscount,
                totalAmount = totalAmount,
                totalCost = totalCost,
                cashReceived = cashReceived,
                changeAmount = changeAmount,
                note = note.trim()
            )
        )

        insertTransactionItems(
            resolvedItems.map { it.copy(transactionId = transactionId) }
        )

        return transactionId
    }

    @Transaction
    suspend fun replaceAllData(
        products: List<ProductEntity>,
        transactions: List<TransactionWithItems>
    ) {
        deleteAllTransactionItems()
        deleteAllTransactions()
        deleteAllProducts()

        if (products.isNotEmpty()) {
            insertProducts(products)
        }
        for (txWithItems in transactions) {
            val newTxId = insertTransaction(txWithItems.transaction)
            val itemsToInsert = txWithItems.items.map { item ->
                item.copy(id = 0L, transactionId = newTxId)
            }
            if (itemsToInsert.isNotEmpty()) {
                insertTransactionItems(itemsToInsert)
            }
        }
    }

    @Transaction
    suspend fun mergeBackupData(
        products: List<ProductEntity>,
        transactions: List<TransactionWithItems>
    ): Pair<Int, Int> {
        var productsAddedOrUpdated = 0
        var transactionsAdded = 0
        val idMapping = mutableMapOf<Long, Long>()

        for (incoming in products) {
            val existing = getProductByName(incoming.name)
            if (existing != null) {
                val updated = existing.copy(
                    category = incoming.category,
                    costPrice = incoming.costPrice,
                    sellingPrice = incoming.sellingPrice,
                    stock = incoming.stock,
                    unit = incoming.unit,
                    updatedAt = maxOf(existing.updatedAt, incoming.updatedAt)
                )
                updateProduct(updated)
                idMapping[incoming.id] = existing.id
            } else {
                val newId = insertProduct(incoming.copy(id = 0L))
                idMapping[incoming.id] = newId
            }
            productsAddedOrUpdated++
        }

        for (txWithItems in transactions) {
            val tx = txWithItems.transaction
            if (!invoiceExists(tx.invoiceNumber)) {
                val newTxId = insertTransaction(tx.copy(id = 0L))
                val items = txWithItems.items.map { item ->
                    item.copy(
                        id = 0L,
                        transactionId = newTxId,
                        productId = item.productId?.let { oldId -> idMapping[oldId] ?: oldId }
                    )
                }
                if (items.isNotEmpty()) {
                    insertTransactionItems(items)
                }
                transactionsAdded++
            }
        }

        return productsAddedOrUpdated to transactionsAdded
    }
}
