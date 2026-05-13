package com.example.pricetracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

 //ProductsFragment - Екран зі списком розслідуваних товарів.
class ProductsFragment : Fragment() {

    // ЗМІННІ
    private lateinit var adapter: ProductAdapter
    private lateinit var productDao: ProductDao
    private lateinit var recyclerView: RecyclerView

    // Зберігаємо список, щоб знати, чи є що видаляти (для кнопки "Очистити все")
    private var currentProducts: List<ProductEntity> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Вмикаємо верхнє меню (кошик)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Готуємо базу даних
        initDatabase()

        // 2. Знаходимо елементи на екрані
        initViews(view)

        // 3. Налаштовуємо список (RecyclerView)
        setupRecyclerView()

        // 4. Вмикаємо "спостереження" за даними (авто-оновлення)
        setupDataObservation()
    }

    // ДОПОМІЖНІ ФУНКЦІЇ

    // Крок 1: Отримуємо доступ до БД
    private fun initDatabase() {
        productDao = AppDatabase.getDatabase(requireContext()).productDao()
    }

    // Крок 2: Знаходимо View
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.productsRecyclerView)
    }

    // Крок 3: Налаштовуємо Адаптер і Список
    private fun setupRecyclerView() {
        // Створюємо адаптер і передаємо йому дві дії (що робити при кліку):
        adapter = ProductAdapter(
            onUrlClick = { url -> openUrlInBrowser(url) },
            onDelete = { product -> deleteProduct(product) }
        )

        // Кажемо списку, як розміщувати елементи (вертикально)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    // Крок 4: Налаштовуємо авто-оновлення списку
    private fun setupDataObservation() {
        // LiveData автоматично повідомить нас, коли зміниться таблиця товарів
        productDao.getAllProductsLive().observe(viewLifecycleOwner) { products ->
            currentProducts = products
            adapter.updateProducts(products) // Оновлюємо адаптер новими даними
        }
    }

    // ДІЇ КОРИСТУВАЧА

    // Відкрити посилання
    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Не вдалося відкрити посилання", Toast.LENGTH_SHORT).show()
        }
    }

    // Видалити товар
    private fun deleteProduct(product: ProductEntity) {
        // Робота з БД завжди у фоновому потоці (IO)
        lifecycleScope.launch(Dispatchers.IO) {
            productDao.deleteById(product.id)

            // Повідомлення показуємо на головному потоці (Main)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Товар видалено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Кнопка Видалити все

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.products_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear) {
            checkAndShowClearDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkAndShowClearDialog() {
        if (currentProducts.isEmpty()) {
            Toast.makeText(requireContext(), "Список вже порожній", Toast.LENGTH_SHORT).show()
        } else {
            showConfirmDeleteAllDialog()
        }
    }

    // Показуємо діалог "Ви впевнені?"
    private fun showConfirmDeleteAllDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Очищення")
            .setMessage("Ви точно хочете видалити всі товари?")
            .setNegativeButton("Ні", null)
            .setPositiveButton("Так") { _, _ ->
                performDeleteAll()
            }
            .show()
    }

    private fun performDeleteAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            productDao.deleteAll()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Всі товари видалено", Toast.LENGTH_SHORT).show()
            }
        }
    }
}