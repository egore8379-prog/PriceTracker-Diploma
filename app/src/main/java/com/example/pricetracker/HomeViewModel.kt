package com.example.pricetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * HomeViewModel - це "мозок" головного екрану.
 * Він відповідає за логіку: додавання товару, збереження в базу, спілкування з сервером.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    // 1. Скринька для статусу завантаження.
    // MutableLiveData - це як "контейнер", за яким можна спостерігати.
    // (false) означає, що початкове значення: "не завантажується".
    private val _loading = MutableLiveData(false)

    val loading: LiveData<Boolean> get() = _loading

    // 2. Скринька для текстових повідомлень ("Додаємо...").
    // (null) означає, що спочатку тексту немає.
    private val _status = MutableLiveData<String?>(null)
    val status: LiveData<String?> get() = _status

    // Підключаємо наш (API) та "пам'ять" (Базу Даних)
    private val apiService = PriceApi.create(Constants.SERVER_URL)
    private val database = AppDatabase.getDatabase(app)

    /**
     * Основна функція додавання товару.
     * url - посилання на товар, яке ввів користувач.
     */
    fun addProduct(url: String) {
        // Увімкнули "крутилку" завантаження
        _loading.value = true
        _status.value = "Додаємо товар..."

        // viewModelScope.launch - це запуск "корутини" (легкого потоку).
        // Не можемо робити запити в інтернет в головному потоці (бо додаток зависне), тому робимо це у фоні.
        viewModelScope.launch {
            try {
                // 1. ЗАПИТ ДО СЕРВЕРА (отримуємо назву і ціну)
                val parsedProductData = withContext(Dispatchers.IO) {
                    apiService.parseProduct(ParseRequest(url))
                }

                // 2. ПІДГОТОВКА ДО ЗБЕРЕЖЕННЯ
                // Створюємо об'єкт для бази даних
                val productToSave = ProductEntity(
                    url = url,
                    name = parsedProductData.name,
                    currentPrice = parsedProductData.currentPrice,
                    oldPrice = parsedProductData.oldPrice,
                    lastChecked = System.currentTimeMillis() // Поточний час
                )

                // 3. ЗБЕРЕЖЕННЯ В БАЗУ ДАНИХ
                withContext(Dispatchers.IO) {
                    database.productDao().insert(productToSave)
                }

                // 4. УСПІХ
                // Вимикаємо "крутилку", показуємо повідомлення
                _loading.value = false
                _status.value = "Товар додано!"

                // Запускаємо фонового працівника (Worker), щоб він перевіряв ціни далі сам
                scheduleBackgroundPriceCheck()

            } catch (e: Exception) {
                // ЯКЩО ЩОСЬ ПІШЛО НЕ ТАК (наприклад, нема інтернету)
                _loading.value = false
                _status.value = "Помилка: Не вдалося завантажити"
            }
        }
    }

    // Очищаємо статус, щоб повідомлення не показувалось двічі
    fun clearStatus() {
        _status.value = null
    }

    /**
     * Запускає періодичну перевірку цін у фоні.
     * Навіть якщо додаток закритий, Android буде намагатися запускати це раз на годину.
     */
    private fun scheduleBackgroundPriceCheck() {
        // Створюємо запит на роботу: "Повторювати кожну 1 годину"
        val workRequest = PeriodicWorkRequestBuilder<GlobalPriceCheckWorker>(1, TimeUnit.HOURS).build()

        // Ставимо в чергу.
        // ExistingPeriodicWorkPolicy.KEEP означає: "Якщо така робота вже запланована, то не чіпати її, хай працює".
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            Constants.WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}