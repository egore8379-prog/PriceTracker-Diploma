package com.example.pricetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ProductEntity (Таблиця "products")
 * Це опис того, як виглядає один "товар" у нашій базі даних.
 */
@Entity(tableName = "products")
data class ProductEntity(

    // 1. УНІКАЛЬНИЙ НОМЕР (ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // 2. ОСНОВНА ІНФОРМАЦІЯ
    val url: String,
    val name: String? = null,
    val currentPrice: String? = null,
    val oldPrice: String? = null,
    val lastChecked: Long = 0L
)