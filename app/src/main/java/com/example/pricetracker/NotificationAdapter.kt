package com.example.pricetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 *       NOTIFICATION ADAPTER (Адаптер)
 *
 * Цей файл відповідає за те, як виглядає СПИСОК сповіщень.
 */
class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // ЗМІННІ КЛАСУ

    // Наш список даних. Спочатку він порожній.
    private var notificationsList: List<NotificationEntity> = listOf()

    // ВНУТРІШНІЙ КЛАС ViewHolder

    /**
     * NotificationViewHolder - "контейнер" для одного запису.
     * Тримає посилання на текст заголовка, повідомлення і дати.
     */
    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val textDate: TextView = view.findViewById(R.id.textDate)
    }

    // ГОЛОВНИЙ МЕТОД ОНОВЛЕННЯ

    /**
     * submitList - Цей метод ми викликаємо з екрану (Fragment),
     * коли отримали нові дані з бази.
     */
    fun submitList(newList: List<NotificationEntity>) {
        notificationsList = newList
        notifyDataSetChanged()
    }

    // ОБОВ'ЯЗКОВІ МЕТОДИ

    // 1. Скільки в нас записів
    override fun getItemCount(): Int {
        return notificationsList.size
    }

    // 2. Створити нову "формочку" (картку)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    // 3. Заповнити "формочку" текстом
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        // Беремо конкретне сповіщення зі списку
        val item = notificationsList[position]

        // Записуємо текст у поля
        holder.textTitle.text = item.title
        holder.textMessage.text = item.message

        // Форматуємо дату (наприклад: "25 Січ, 14:30")
        val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        holder.textDate.text = formatter.format(Date(item.timestamp))
    }
}