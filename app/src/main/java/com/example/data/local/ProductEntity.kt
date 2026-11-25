package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String,
    val costPrice: Long,
    val sellingPrice: Long,
    val stock: Int,
    val unit: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val estimatedProfitPerUnit: Long
        get() = sellingPrice - costPrice

    val marginPercent: Int
        get() = if (sellingPrice > 0L) {
            (((sellingPrice - costPrice).toDouble() / sellingPrice.toDouble()) * 100.0).toInt()
        } else {
            0
        }
}
