package com.example.pricetracker

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * ProductDao (Data Access Object)

 * Це "посередник" між нашим кодом (Kotlin) і базою даних (SQL).
 * Ми кажемо йому "дай товари" або "видали товар", а він перекладає це на мову бази даних.
 */
@Dao
interface ProductDao {

    // 1. ДОДАВАННЯ (Create)

    /**
     * Додає новий товар у базу.
     * @param product - об'єкт товару, який ми хочемо зберегти
     * @return Long - номер (ID), під яким запис зберігся в таблиці
     */
    @Insert
    suspend fun insert(product: ProductEntity): Long

    /**
     * Додає список товарів у базу (для імпорту).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    // 2. ЧИТАННЯ (Read)

    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProductsLive(): LiveData<List<ProductEntity>>

    /**
     * Отримує ВСІ товари для фонової перевірки цін (Worker).

     */
    @Query("SELECT * FROM products ORDER BY id DESC")
    suspend fun getAllProducts(): List<ProductEntity>

    /**
     * Шукає один конкретний товар за його унікальним номером (ID).
     */
    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): ProductEntity?

    // 3. ОНОВЛЕННЯ (Update)

    /**
     * Оновлює існуючий товар.
     */
    @Update
    suspend fun update(product: ProductEntity)

    // 4. ВИДАЛЕННЯ (Delete)

    /**
     * Видаляє один товар конкретно за його ID.
     */
    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Int)

    /**
     * Видаляє АБСОЛЮТНО ВСІ записи з таблиці.
     */
    @Query("DELETE FROM products")
    suspend fun deleteAll()
}