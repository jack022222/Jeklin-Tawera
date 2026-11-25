package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CheckoutLineRequest
import com.example.data.local.PosDatabase
import com.example.data.local.ProductEntity
import com.example.data.repository.BackupImportMode
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: PosDatabase
    private lateinit var repository: PosRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PosRepository(database.posDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KasirKu", appName)
    }

    @Test
    fun `product CRUD and atomic sale stock deduction`() = runBlocking {
        // Create
        val productId = repository.insertProduct(
            ProductEntity(
                name = "Kopi Susu",
                category = "Minuman",
                costPrice = 8000L,
                sellingPrice = 15000L,
                stock = 10,
                unit = "cup"
            )
        )
        var products = repository.allProducts.first()
        assertEquals(1, products.size)
        assertEquals("Kopi Susu", products.first().name)

        // Update
        repository.updateProduct(products.first().copy(sellingPrice = 16000L))
        products = repository.allProducts.first()
        assertEquals(16000L, products.first().sellingPrice)

        // Atomic checkout (2 cups @ 16000 = 32000, discount 2000 => total 30000, cash 50000 => change 20000)
        val receipt = repository.completeSale(
            requests = listOf(CheckoutLineRequest(productId, "Kopi Susu", 2)),
            discountAmount = 2000L,
            cashReceived = 50000L,
            note = "Meja 1"
        )
        assertEquals(32000L, receipt.transaction.subtotal)
        assertEquals(30000L, receipt.transaction.totalAmount)
        assertEquals(16000L, receipt.transaction.totalCost)
        assertEquals(14000L, receipt.transaction.grossProfit)
        assertEquals(20000L, receipt.transaction.changeAmount)

        // Verify stock automatically decremented from 10 to 8
        products = repository.allProducts.first()
        assertEquals(8, products.first().stock)

        // Delete product
        repository.deleteProduct(products.first())
        assertTrue(repository.allProducts.first().isEmpty())
        // Historical receipt still intact
        assertEquals(1, repository.allTransactions.first().size)
    }

    @Test
    fun `json and csv backup export and import`() = runBlocking {
        val id = repository.insertProduct(
            ProductEntity(
                name = "Roti Bakar",
                category = "Makanan",
                costPrice = 6000L,
                sellingPrice = 12000L,
                stock = 5,
                unit = "porsi"
            )
        )
        repository.completeSale(
            requests = listOf(CheckoutLineRequest(id, "Roti Bakar", 1)),
            discountAmount = 0L,
            cashReceived = 15000L,
            note = ""
        )

        val jsonBackup = repository.exportFullBackupJson()
        val productsCsv = repository.exportProductsCsv()
        val txCsv = repository.exportTransactionsCsv()
        assertTrue(productsCsv.contains("Roti Bakar"))
        assertTrue(txCsv.contains("Roti Bakar"))

        // Replace restore
        val summary = repository.importFullBackupJson(jsonBackup, BackupImportMode.REPLACE)
        assertEquals(1, summary.productsCount)
        assertEquals(1, summary.transactionsCount)
        assertEquals(4, repository.allProducts.first().first().stock)

        // Import additional product via CSV
        val importedCsvCount = repository.importProductsCsv(
            "nama,kategori,harga_beli_hpp,harga_jual,stok,satuan\nTeh Manis,Minuman,2000,5000,20,gelas"
        )
        assertEquals(1, importedCsvCount)
        assertEquals(2, repository.allProducts.first().size)
    }
}
