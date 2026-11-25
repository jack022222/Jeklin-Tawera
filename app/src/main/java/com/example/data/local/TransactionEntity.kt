package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val invoiceNumber: String,
    val timestamp: Long,
    val subtotal: Long,
    val discountAmount: Long,
    val totalAmount: Long,
    val totalCost: Long,
    val cashReceived: Long,
    val changeAmount: Long,
    val note: String = ""
) {
    val grossProfit: Long
        get() = totalAmount - totalCost
}

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transactionId: Long,
    val productId: Long?,
    val productName: String,
    val category: String,
    val unit: String,
    val unitCostPrice: Long,
    val unitSellingPrice: Long,
    val quantity: Int,
    val lineTotal: Long
)

data class TransactionWithItems(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItemEntity>
)
