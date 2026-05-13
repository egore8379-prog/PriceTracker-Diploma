package com.example.pricetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProductEntity::class, NotificationEntity::class], // Список наших таблиць
    version = 1,                                                  // Версія
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {

            // КРОК 1: Перевіряємо, чи база вже створена
            val currentInstance = INSTANCE
            if (currentInstance != null) {
                return currentInstance // Якщо є, одразу повертаємо її
            }

            // КРОК 2: Якщо бази немає, починаємо створювати
            // synchronized означає "тільки один потік одночасно", щоб не створити дві бази
            synchronized(this) {

                // Ще раз перевіряємо
                val checkAgain = INSTANCE
                if (checkAgain != null) {
                    return checkAgain
                }

                // КРОК 3: Створюємо базу даних
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database" // Ім'я файлу на телефоні
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = newInstance // Запам'ятовуємо створену базу
                return newInstance
            }
        }
    }
}