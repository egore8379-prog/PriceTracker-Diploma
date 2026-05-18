package com.example.pricetracker

// ПРИЗНАЧЕННЯ: Робота з сервером для отримання цін товарів

import okhttp3.OkHttpClient          // HTTP-клієнт для запитів
import retrofit2.Retrofit             // Головна бібліотека для API
import retrofit2.converter.gson.GsonConverterFactory  // Конвертер JSON ↔ Kotlin об'єкти
import retrofit2.http.Body            // Анотація для тіла запиту
import retrofit2.http.POST            // Анотація для POST-запиту
import java.util.concurrent.TimeUnit  // Одиниці часу (секунди, хвилини)

// КРОК 1: МОДЕЛЬ ЗАПИТУ (що ми ВІДПРАВЛЯЄМО на сервер)

data class ParseRequest(
    val url: String  // URL-адреса товару, ціну якого хочемо дізнатись
)

// КРОК 2: МОДЕЛЬ ВІДПОВІДІ (що ми ОТРИМУЄМО від сервера)

data class ParseResponse(
    val name: String,          // Назва товару (наприклад: "iPhone 15 Pro")
    val currentPrice: String,  // Поточна ціна (наприклад: "49999 грн")
)

// КРОК 3: ІНТЕРФЕЙС API (опис запитів до сервера)

interface PriceApi {

    // Метод для отримання ціни товару
    @POST("parse")  // Запит йде на адресу: baseUrl + "parse"
    suspend fun parseProduct(
        @Body req: ParseRequest  // @Body означає що req буде в тілі запиту як JSON
    ): ParseResponse  // Повертає об'єкт ParseResponse


    // COMPANION OBJECT - місце для статичних методів
    companion object {

        // Метод для створення екземпляру API
        fun create(baseUrl: String): PriceApi {

            // НАЛАШТУВАННЯ HTTP-КЛІЄНТА
            // Таймаути потрібні щоб запит не "висів" вічно якщо сервер не відповідає
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)  // Час на підключення до сервера
                .readTimeout(180, TimeUnit.SECONDS)     // Час очікування відповіді
                .writeTimeout(120, TimeUnit.SECONDS)    // Час на відправку даних
                .build()

            // СТВОРЕННЯ RETROFIT ОБ'ЄКТА
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)                       // Базова адреса сервера
                .client(httpClient)                     // Наш HTTP-клієнт
                .addConverterFactory(                   // Конвертер JSON ↔ Kotlin
                    GsonConverterFactory.create()
                )
                .build()

            // СТВОРЕННЯ API ІНТЕРФЕЙСУ
            // Retrofit автоматично створює реалізацію нашого інтерфейсу
            return retrofit.create(PriceApi::class.java)
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// 📋 ШПАРГАЛКА: ЯК ЦЕ ВСЕ ПРАЦЮЄ РАЗОМ
// ═══════════════════════════════════════════════════════════════════════════════
//
// 1. Ми створюємо API:
//    val api = PriceApi.create("http://192.168.1.100:5000/")
//
// 2. Створюємо запит:
//    val request = ParseRequest(url = "https://rozetka.com.ua/телефон")
//
// 3. Викликаємо метод API (в корутіні):
//    val response = api.parseProduct(request)
//
// 4. Отримуємо дані:
//    println(response.name)          // "iPhone 15 Pro"
//    println(response.currentPrice)  // "49999 грн"
//
// ═══════════════════════════════════════════════════════════════════════════════
//
// 🔄 СХЕМА РОБОТИ:
//
//  [Додаток]              [Сервер]
//      │                      │
//      │ ──── ParseRequest ──→│  (відправляємо URL товару)
//      │                      │
//      │←── ParseResponse ────│  (отримуємо назву та ціну товару)
//      │                      │
//
// ═══════════════════════════════════════════════════════════════════════════════
