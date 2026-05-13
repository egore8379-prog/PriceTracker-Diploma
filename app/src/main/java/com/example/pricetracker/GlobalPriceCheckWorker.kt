package com.example.pricetracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
class GlobalPriceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // ЗМІННІ КЛАСУ
    private val database = AppDatabase.getDatabase(context)

    // API сервіс - це "браузер", який ходить на сайти магазинів і парсить ціни
    private val apiService = PriceApi.create(Constants.SERVER_URL)

    // ГОЛОВНИЙ МЕТОД (тут починається вся робота)
    override suspend fun doWork(): Result {
        // Виконуємо роботу на окремому потоці (щоб не "заморозити" телефон)
        return withContext(Dispatchers.IO) {
            try {
                // Перевіряємо ціни
                checkAllProducts()
                // Повертаємо успіх
                Result.success()
            } catch (e: Exception) {
                // Якщо щось пішло не так - логуємо помилку
                Log.e("Worker", "Помилка в doWork: ${e.message}")
                Result.failure()
            }
        }
    }

    // КРОК 1: ПЕРЕВІРКА ВСІХ ТОВАРІВ

    private suspend fun checkAllProducts() {
        // Дістаємо всі товари з бази даних
        val allProducts = database.productDao().getAllProducts()

        // Якщо товарів немає - виходимо (нічого перевіряти)
        if (allProducts.isEmpty()) {
            Log.d("Worker", "Немає товарів для перевірки")
            return
        }

        Log.d("Worker", "Знайдено ${allProducts.size} товарів для перевірки")

        // Перевіряємо кожен товар по черзі
        for (product in allProducts) {
            checkOneProduct(product)
        }
    }

    // КРОК 2: ПЕРЕВІРКА ОДНОГО ТОВАРУ

    private suspend fun checkOneProduct(product: ProductEntity) {
        try {
            // 1. Запитуємо свіжу інформацію з сервера
            Log.d("Worker", "Перевіряю: ${product.name}")
            val freshData = apiService.parseProduct(ParseRequest(product.url))

            // 2. Порівнюємо стару і нову ціну
            val priceChanged = comparePrices(product, freshData)

            // 3. Оновлюємо товар в базі даних
            updateProductInDatabase(product, freshData, priceChanged)

            // 4. Якщо ціна змінилася - показуємо сповіщення
            if (priceChanged) {
                notifyUserAboutPriceChange(product, freshData)
            }

            // 5. Робимо паузу 2 секунди перед наступним запитом
            // (щоб не перевантажувати сервер)
            delay(2000)

        } catch (e: Exception) {
            // Якщо сталася помилка - просто логуємо і йдемо далі
            Log.e("Worker", "Не вдалося перевірити ${product.name}: ${e.message}")
        }
    }

    // КРОК 3: ПОРІВНЯННЯ ЦІН

    /**
     * Порівнюємо стару ціну (з бази) і нову ціну (з сервера)
     * Повертає: true якщо ціна змінилася, false якщо залишилася такою ж
     */
    private fun comparePrices(oldProduct: ProductEntity, newData: ParseResponse): Boolean {
        // Перетворюємо обидві ціни в числа
        val oldPriceNumber = convertPriceToNumber(oldProduct.currentPrice)
        val newPriceNumber = convertPriceToNumber(newData.currentPrice)

        // Якщо не вдалося отримати нову ціну - вважаємо що нічого не змінилося
        if (newPriceNumber == null) {
            return false
        }

        // Якщо старої ціни не було - це не зміна (це перша перевірка)
        if (oldPriceNumber == null) {
            return false
        }

        // Порівнюємо числа: якщо різні - ціна змінилася!
        return oldPriceNumber != newPriceNumber
    }

    // КРОК 4: ОНОВЛЕННЯ ТОВАРУ В БАЗІ ДАНИХ
    /**
     * Записуємо оновлену інформацію в базу даних
     */
    private suspend fun updateProductInDatabase(
        oldProduct: ProductEntity,
        freshData: ParseResponse,
        priceChanged: Boolean
    ) {
        // Визначаємо, яку назву зберігати
        val newName = if (freshData.name.isNotBlank() && freshData.name != "Невідома назва") {
            freshData.name
        } else {
            oldProduct.name // Якщо нова назва погана - залишаємо стару
        }

        // Визначаємо, яку поточну ціну зберігати
        val newCurrentPrice = if (freshData.currentPrice.isNotBlank()) {
            freshData.currentPrice
        } else {
            oldProduct.currentPrice // Якщо нова ціна пуста - залишаємо стару
        }

        // Визначаємо, яку стару ціну зберігати
        val newOldPrice = if (priceChanged) {
            // Якщо ціна змінилася - те що було "поточним" стає "старим"
            oldProduct.currentPrice
        } else {
            // Якщо не змінилася - залишаємо як є
            oldProduct.oldPrice
        }

        // Створюємо оновлену версію товару
        val updatedProduct = oldProduct.copy(
            name = newName,
            currentPrice = newCurrentPrice,
            oldPrice = newOldPrice,
            lastChecked = System.currentTimeMillis() // Зберігаємо час останньої перевірки
        )

        // Записуємо в базу даних
        database.productDao().update(updatedProduct)
    }

    // КРОК 5: СПОВІЩЕННЯ КОРИСТУВАЧА
    /**
     * Показуємо користувачу сповіщення про зміну ціни
     */
    private suspend fun notifyUserAboutPriceChange(oldProduct: ProductEntity, freshData: ParseResponse) {
        // Формуємо текст повідомлення
        val message = "${oldProduct.name}: нова ціна ${freshData.currentPrice} (було ${oldProduct.currentPrice})"

        // Показуємо сповіщення на телефоні
        showNotification("Ціна змінилася!", message)

        // Зберігаємо сповіщення в історію (в базу даних)
        saveNotificationToHistory(message)
    }

    /**
     * Зберігаємо сповіщення в історію (щоб користувач міг подивитися пізніше)
     */
    private suspend fun saveNotificationToHistory(message: String) {
        val notification = NotificationEntity(
            title = "Зміна ціни",
            message = message,
            timestamp = System.currentTimeMillis()
        )
        database.notificationDao().addNotification(notification)
    }

    /**
     * Показуємо сповіщення в "шторці" телефону
     */
    private fun showNotification(title: String, message: String) {
        // ID каналу для сповіщень
        val channelId = "price_tracker_channel"

        // Отримуємо менеджер сповіщень
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Для Android 8.0+ потрібно створити канал сповіщень
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Price Tracker",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Створюємо саме сповіщення
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_price) // Іконка
            .setContentTitle(title) // Заголовок
            .setContentText(message) // Текст
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Високий пріоритет
            .setAutoCancel(true) // Автоматично зникає після кліку
            .build()

        // Показуємо сповіщення (використовуємо час як ID, щоб кожне було унікальним)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // ДОПОМІЖНІ ФУНКЦІЇ (Utilities)
    /**
     * Перетворює ціну з тексту в число
     *
     * Наприклад:
     * "1 299,50 грн" -> 1299.5
     * "2500" -> 2500.0
     * "abc" -> null
     */
    private fun convertPriceToNumber(priceText: String?): Double? {
        // Якщо ціна пуста - повертаємо null
        if (priceText.isNullOrBlank()) {
            return null
        }

        // Видаляємо всі символи крім цифр, коми і крапки
        // Наприклад: "1 299,50 грн" -> "1299,50"
        val cleanPrice = priceText.replace(Regex("[^0-9,.]"), "")

        // Замінюємо кому на крапку (щоб стандартизувати формат)
        // "1299,50" -> "1299.50"
        val standardPrice = cleanPrice.replace(",", ".")

        // Перетворюємо на число
        return standardPrice.toDoubleOrNull()
    }
}