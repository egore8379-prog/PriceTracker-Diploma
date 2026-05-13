package com.example.pricetracker

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**

 * - Це "посередник" між нашим кодом і таблицею сповіщень.
 * - Ми кажемо "дай історію" або "видали все", а Room генерує SQL-код.
 */
@Dao
interface NotificationDao {

    // 1. ДОДАВАННЯ (Create)

    /**
     * Зберігає нове сповіщення в базу.
     *
     * @param notification - об'єкт сповіщення (заголовок, текст, час)
     * 'suspend' - бо запис на диск займає час, тому робимо це у фоні.
     */
    @Insert
    suspend fun addNotification(notification: NotificationEntity)

    //  2. ЧИТАННЯ (Read)

    /**
     * Отримує ВСЮ історію сповіщень.
     * Сортування: найновіші зверху (ORDER BY timestamp DESC).
     * - Повертає LiveData. Це означає, що як тільки прийде нове сповіщення,
     *   список на екрані "NotificationsFragment" оновиться сам.
     */
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): LiveData<List<NotificationEntity>>

    /**
     * Отримує ВСЮ історію сповіщень (сихронно, для бекапу).
     */
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getAllNotificationsSync(): List<NotificationEntity>

    /**
     * Зберігає список сповіщень в базу (для імпорту).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotifications(notifications: List<NotificationEntity>)

    // 3. ВИДАЛЕННЯ (Delete)
    /**
     * Видаляє АБСОЛЮТНО ВСІ записи з таблиці сповіщень.
     * Використовується для кнопки "Очистити історію".
     */
    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}