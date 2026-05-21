package com.example.pricetracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class AccountFragment : Fragment() {

    // ЗМІННІ
    private lateinit var cardAbout: MaterialCardView
    private lateinit var cardContact: MaterialCardView
    private lateinit var cardBackup: MaterialCardView

    // ЛАНЧЕРИ ДЛЯ РОБОТИ З ФАЙЛАМИ

    // Ланчер для створення файлу (Експорт)
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            exportDataToFile(uri)
        }
    }

    // Ланчер для вибору файлу (Імпорт)
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importDataFromFile(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Чітка структура по кроках:

        // 1. Знаходимо елементи (картки)
        initViews(view)

        // 2. Налаштовуємо натискання (Listeners)
        setupListeners()
    }

    // ДОПОМІЖНІ ФУНКЦІЇ

    // Крок 1: Знаходимо View
    private fun initViews(view: View) {
        cardAbout = view.findViewById(R.id.cardAbout)
        cardContact = view.findViewById(R.id.cardContact)
        cardBackup = view.findViewById(R.id.cardBackup)
    }

    // Крок 2: Налаштовуємо дії
    private fun setupListeners() {
        // Кнопка "Про застосунок"
        cardAbout.setOnClickListener {
            showAboutDialog()
        }

        // Кнопка "Написати нам"
        cardContact.setOnClickListener {
            sendEmailToDeveloper()
        }

        // Кнопка "Резервна копія"
        cardBackup.setOnClickListener {
            showBackupOptionsDialog()
        }
    }

    // ЛОГІКА ІНТЕРФЕЙСУ

    // Показує вікно "Про додаток"
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("PriceTracker")
            .setMessage(R.string.about_app_text)
            .setPositiveButton("Зрозуміло", null)
            .show()
    }

    // Відкриває поштовий клієнт
    private fun sendEmailToDeveloper() {
        val email = "8051658@stud.kai.edu.ua"
        val subject = "PriceTracker: Відгук"

        // Створюємо намір "Відправити" (ACTION_SENDTO)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email") // Тільки поштові програми
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Поштова програма не знайдена", Toast.LENGTH_SHORT).show()
        }
    }

    // НОВА ЛОГІКА: БЕКАП І ВІДНОВЛЕННЯ

    private fun showBackupOptionsDialog() {
        val options = arrayOf("Створити локальну копію (Експорт)", "Відновити з файлу (Імпорт)")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Резервне копіювання")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Експорт
                        exportLauncher.launch("PriceTracker_Backup.json")
                    }
                    1 -> {
                        // Імпорт
                        importLauncher.launch(arrayOf("application/json"))
                    }
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    // ЕКСПОРТ ДАНИХ У ФАЙЛ
    private fun exportDataToFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(requireContext())
                val products = database.productDao().getAllProducts()
                val notifications = database.notificationDao().getAllNotificationsSync()

                val backupData = BackupData(products, notifications)
                val jsonString = Gson().toJson(backupData)

                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Бекап успішно збережено!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Помилка експорту: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ІМПОРТ ДАНИХ З ФАЙЛУ
    private fun importDataFromFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Читаємо JSON з файлу
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val reader = InputStreamReader(inputStream)
                val backupData = Gson().fromJson(reader, BackupData::class.java)
                reader.close()
                inputStream?.close()

                if (backupData != null) {
                    val database = AppDatabase.getDatabase(requireContext())

                    // Зберігаємо дані в базу
                    database.productDao().insertAll(backupData.products)
                    database.notificationDao().insertAllNotifications(backupData.notifications)

                    // Запускаємо WorkManager для відновлених товарів
                    scheduleBackgroundPriceCheck()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Дані успішно відновлено!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Файл порожній або пошкоджений", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Помилка імпорту: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Запускає періодичну перевірку цін у фоні.
     * Аналогічно тому, як це робиться в HomeViewModel при додаванні НОВОГО товару.
     */
    private fun scheduleBackgroundPriceCheck() {
        val workRequest = PeriodicWorkRequestBuilder<GlobalPriceCheckWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            Constants.WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}