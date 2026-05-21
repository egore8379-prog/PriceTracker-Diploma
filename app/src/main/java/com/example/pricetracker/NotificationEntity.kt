package com.example.pricetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NotificationEntity - ОПИС ОДНОГО РЯДКА В ТАБЛИЦІ

 * У базі даних Room  не створюємо таблиці SQL-кодом (як CREATE TABLE...).
 * Замість цього  створюємо звичайний клас і ставимо над ним @Entity.
 * Кожен екземпляр цього класу = Один рядок у таблиці "notifications".
 * @Entity(tableName = "notifications") -> це назва таблиці в базі даних.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(

    // ГОЛОВНИЙ ІДЕНТИФІКАТОР (Passport ID)

    /**
     * @PrimaryKey - Це унікальний номер кожного запису.
     * autoGenerate = true - означає, що база сама придумає номер (1, 2, 3...), не треба про це думати.
     * id = 0 - початкове значення (коли записуємо в базу, Room його ігнорує і ставить свій номер).
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // ІНФОРМАЦІЯ ПРО СПОВІЩЕННЯ (Колонки таблиці)

    // Заголовок сповіщення (наприклад: "Ціна змінилася!")
    val title: String,

    // Текст повідомлення (наприклад: "iPhone 13 тепер коштує 25000 грн")
    val message: String,

    // Час створення (зберігаємо як велике число Long - кількість мілісекунд з 1970 року)
    // Це стандартний спосіб зберігати час у комп'ютерах.
    val timestamp: Long
)