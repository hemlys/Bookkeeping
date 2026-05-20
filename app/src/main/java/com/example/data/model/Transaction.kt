package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a financial ledger entry.
 *
 * @property id Unique identifier for Room database. Set to 0 for auto-generation.
 * @property type Either "EXPENSE" or "INCOME".
 * @property category Category of the transaction (e.g. 午餐, 薪資, 娛樂).
 * @property amount Numeric value/volume. Always positive.
 * @property date Epoch timestamp in milliseconds.
 * @property note Custom remark or memorandum.
 */
@Entity(tableName = "transactions")
@JsonClass(generateAdapter = true)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    @Json(name = "id")
    val id: Int = 0,
    
    @Json(name = "type")
    val type: String, // "EXPENSE" or "INCOME"
    
    @Json(name = "category")
    val category: String,
    
    @Json(name = "amount")
    val amount: Double,
    
    @Json(name = "date")
    val date: Long = System.currentTimeMillis(),
    
    @Json(name = "note")
    val note: String = ""
) {
    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"
        
        val EXPENSE_CATEGORIES = listOf("餐飲", "交通", "購物", "娛樂", "居住", "醫療", "教育", "投資", "其他")
        val INCOME_CATEGORIES = listOf("薪資", "獎金", "投資收益", "零用錢", "兼職", "其他")
    }
}
