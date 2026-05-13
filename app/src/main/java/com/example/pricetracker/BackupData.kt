package com.example.pricetracker

import androidx.annotation.Keep

@Keep // Анотація, щоб Android не "зламав" цей клас при створенні фінальної версії додатка
data class BackupData(
    val products: List<ProductEntity>,
    val notifications: List<NotificationEntity>
)
