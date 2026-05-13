package com.example.pricetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * NotificationsFragment - Екран, де користувач бачить історію сповіщень.
 */
class NotificationsFragment : Fragment() {

    // ЗМІННІ
    private lateinit var adapter: NotificationAdapter
    private lateinit var notificationDao: NotificationDao

    // Елементи інтерфейсу
    private lateinit var recycler: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnClear: MaterialButton
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Налаштовуємо анімацію появи (Zoom / Scale)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Завантажуємо макет екрану (XML)
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Готуємо базу даних
        initDatabase()

        // 2. Знаходимо всі кнопки та елементи на екрані
        initViews(view)

        // 3. Налаштовуємо список (RecyclerView)
        setupRecyclerView()

        // 4. Налаштовуємо кнопки (що вони роблять при натисканні)
        setupListeners()

        // 5. Починаємо слідкувати за сповіщеннями в базі
        observeNotifications()
    }

    // ДОПОМІЖНІ ФУНКЦІЇ (КРОКИ)

    // Крок 1: Підключаємося до бази даних
    private fun initDatabase() {
        notificationDao = AppDatabase.getDatabase(requireContext()).notificationDao()
    }

    // Крок 2: Знаходимо View
    private fun initViews(view: View) {
        recycler = view.findViewById(R.id.recyclerNotifications)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        btnClear = view.findViewById(R.id.btnClearHistory)
        btnBack = view.findViewById(R.id.btnBack)
    }

    // Крок 3: Налаштовуємо Список
    private fun setupRecyclerView() {
        adapter = NotificationAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    // Крок 4: Налаштовуємо Кнопки
    private fun setupListeners() {
        // Кнопка "Назад"
        btnBack.setOnClickListener {
            // Повертаємося на попередній екран (HomeFragment)
            parentFragmentManager.popBackStack()
        }

        // Кнопка "Очистити історію"
        btnClear.setOnClickListener {
            checkAndShowClearDialog()
        }
    }

    // Крок 5: Спостерігаємо за даними
    private fun observeNotifications() {
        // ".observe" означає, що як тільки в базі щось зміниться, цей код спрацює автоматично
        notificationDao.getAllNotifications().observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateEmptyState(list.isEmpty())
        }
    }

    // ЛОГІКА ІНТЕРФЕЙСУ

    // Показує або ховає напис "Пусто", залежно від того, чи є сповіщення
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recycler.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun checkAndShowClearDialog() {
        if (adapter.itemCount == 0) {
            Toast.makeText(requireContext(), "Історія сповіщень вже порожня", Toast.LENGTH_SHORT).show()
        } else {
            showDeleteConfirmationDialog()
        }
    }

    // Показує діалогове вікно "Ви впевнені?"
    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Очищення")
            .setMessage("Ви точно хочете видалити історію?")
            .setNegativeButton("Ні", null)
            .setPositiveButton("Так") { _, _ ->
                performClearAll()
            }
            .show()
    }

    // Видаляє все з бази даних
    private fun performClearAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            notificationDao.clearAllNotifications()

            // Щоб показати Toast, треба повернутися на Головний потік (Main Thread)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Історія очищена", Toast.LENGTH_SHORT).show()
            }
        }
    }
}