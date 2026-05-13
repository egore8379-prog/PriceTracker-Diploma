package com.example.pricetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * ProductAdapter - АДАПТЕР ДЛЯ СПИСКУ ТОВАРІВ

 * Це "посередник" між даними (список товарів) і візуальним списком на екрані (RecyclerView)
 * Він бере кожен товар і показує його на екрані у вигляді картки
 */
class ProductAdapter(
    private val onUrlClick: (String) -> Unit,  // Функція для відкриття URL
    private val onDelete: (ProductEntity) -> Unit  // Функція для видалення товару
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // ЗМІННІ КЛАСУ

    // Список товарів, які показуємо на екрані
    private var productsList: List<ProductEntity> = listOf()

    // ВНУТРІШНІЙ КЛАС ViewHolder
    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Знаходимо всі елементи з макету item_product.xml
        val nameText: TextView = view.findViewById(R.id.textName)
        val urlText: TextView = view.findViewById(R.id.textUrl)
        val priceText: TextView = view.findViewById(R.id.textPrice)
        val oldPriceText: TextView = view.findViewById(R.id.textOldPrice)
        val deleteButton: Button = view.findViewById(R.id.buttonDelete)
    }

    // МЕТОДИ ДЛЯ РОБОТИ З ДАНИМИ

    /**
     * updateProducts - Оновлює список товарів
     */
    fun updateProducts(newProducts: List<ProductEntity>) {
        productsList = newProducts
        notifyDataSetChanged()  // Оновлюємо відображення списку
    }

    // ОБОВ'ЯЗКОВІ МЕТОДИ АДАПТЕРА
    override fun getItemCount(): Int {
        return productsList.size
    }

    /**
     * onCreateViewHolder - Створити нову картку (без даних)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        // Створюємо View з макету item_product.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)

        // Обгортаємо його в ViewHolder і повертаємо
        return ProductViewHolder(view)
    }

    /**
     * onBindViewHolder - Заповнити картку даними
     */
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        // Крок 1: Отримуємо товар з позиції position
        val product = productsList[position]

        // Крок 2: Заповнюємо текстові поля
        holder.nameText.text = product.name ?: "Невідома назва"
        holder.urlText.text = product.url
        holder.priceText.text = "Ціна: ${product.currentPrice ?: "-"}"

        // Крок 3: Показуємо стару ціну (якщо вона є)
        if (product.oldPrice.isNullOrBlank()) {
            // Якщо старої ціни немає - ховаємо TextView
            holder.oldPriceText.visibility = View.GONE
        } else {
            // Якщо стара ціна є - показуємо її
            holder.oldPriceText.visibility = View.VISIBLE
            holder.oldPriceText.text = "Було: ${product.oldPrice}"
        }

        // Крок 4: Налаштовуємо натискання на URL (відкрити в браузері)
        holder.urlText.setOnClickListener {
            onUrlClick(product.url)
        }

        // Крок 5: Налаштовуємо кнопку видалення
        holder.deleteButton.setOnClickListener {
            onDelete(product)
        }
    }
}