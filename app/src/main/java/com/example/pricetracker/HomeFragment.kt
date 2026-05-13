package com.example.pricetracker

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.transition.MaterialSharedAxis

/**
 * HomeFragment - Екран додавання товарів.
 */
class HomeFragment : Fragment() {

    private lateinit var editTextUrl: EditText
    private lateinit var buttonAdd: Button
    private lateinit var progressAdding: ProgressBar

    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Дозволяємо цьому фрагменту мати свої кнопки в меню зверху
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_home, container, false)

        // Знаходимо елементи інтерфейсу
        editTextUrl = v.findViewById(R.id.editTextUrl)
        buttonAdd = v.findViewById(R.id.buttonAdd)
        progressAdding = v.findViewById(R.id.progressAdding)

        // Слідкуємо за тим, що вводить користувач
        editTextUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Перевіряємо, чи це посилання
                val url = s.toString()
                val isValidUrl = url.isNotEmpty() && Patterns.WEB_URL.matcher(url).matches()
                // Активуємо кнопку тільки якщо це дійсне посилання
                buttonAdd.isEnabled = isValidUrl
            }
        })

        buttonAdd.setOnClickListener {
            val url = editTextUrl.text.toString().trim()
            if (url.isNotEmpty()) viewModel.addProduct(url)
        }

        // ЛОГІКА СПОСТЕРЕЖЕННЯ (Observers)
        // Фрагмент "слухає" зміни у ViewModel. Як тільки дані зміняться, код нижче виконається.

        // 1. Слідкуємо за завантаженням (Крутилка)
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Якщо завантаження йде (true) -> показуємо крутилку (VISIBLE)
            // Якщо ні (false) -> ховаємо (GONE)
            progressAdding.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Вимикаємо кнопку, поки йде завантаження
            buttonAdd.isEnabled = !isLoading && editTextUrl.text.isNotEmpty()
        }

        // 2. Слідкуємо за повідомленнями (Тости)
        viewModel.status.observe(viewLifecycleOwner) { message ->
            // Якщо є повідомлення -> показуємо Toast (спливаюче вікно)
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                if (message.startsWith("Товар додано")) {
                    editTextUrl.text.clear() // Очистити поле, якщо успіх
                }

                // Одразу очищаємо статус у ViewModel, щоб повідомлення не висіло вічно
                viewModel.clearStatus()
            }
        }

        // Якщо нам передали посилання (через "Поділитися" з іншого додатку)
        arguments?.getString("sharedUrl")?.let { url ->
            editTextUrl.setText(url)
        }

        return v
    }

    // Створюємо меню (дзвіночок зверху)
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // Обробка натискання на меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                openNotificationsScreen()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Відкриває екран сповіщень.
     */
    private fun openNotificationsScreen() {
        // Налаштування анімації переходу
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, NotificationsFragment())

             // Це команда системі: "Додай цей перехід у історію".
            // Без цього рядка, ні системна кнопка "Назад" на телефоні, ні кнопка "Back" у NotificationFragment
            // не знатимуть, куди повертатися, і додаток просто закриється.
            .addToBackStack(null)
            .commit()
    }

    // Метод для оновлення URL ззовні (з MainActivity)
    fun updateUrl(url: String) {
        if (this::editTextUrl.isInitialized) {
            editTextUrl.setText(url)
        }
    }
}