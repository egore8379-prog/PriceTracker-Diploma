package com.example.pricetracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity - це "контейнер" нашого додатку.
 * Він тримає в собі все: верхню панель (Toolbar), нижнє меню (BottomNav) і місце для екранів (FragmentContainer).
 * Самі екрани (Home, Products, Account) - це Фрагменти, які підміняються всередині Activity.
 */
class MainActivity : AppCompatActivity() {

    // ЗМІННІ
    // Це елементи інтерфейсу. "lateinit" означає, що ми знайдемо їх трохи пізніше (в onCreate).
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Знаходимо кнопки та меню
        initViews()

        // 2. Налаштовуємо нижнє меню (навігацію)
        setupBottomNavigation()

        // 3. Налаштовуємо, коли ховати меню (наприклад, на екрані сповіщень)
        setupVisibilityLogic()

        // 4. Перевіряємо, чи є дозволи на сповіщення
        checkAndRequestPermissions()

        // 5. Завантажуємо перший екран (якщо це перший запуск)
        if (savedInstanceState == null) {
            setupInitialFragment()
        }

        // 6. Якщо додаток відкрили через "Поділитися посиланням"
        handleIncomingIntent(intent)
    }

    // ДОПОМІЖНІ ФУНКЦІЇ

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // Кажемо системі, що це наш головний тулбар

        bottomNav = findViewById(R.id.bottom_nav)
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            // Дивимося, яка кнопка натиснута, і створюємо відповідний фрагмент
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_products -> ProductsFragment()
                R.id.nav_account -> AccountFragment()
                else -> null
            }

            // Якщо фрагмент вибрано успішно - завантажуємо його
            if (selectedFragment != null) {
                loadFragment(selectedFragment)
                true // true означає: "Так, зроби цю кнопку активною (засвіти її)"
            } else {
                false
            }
        }
    }

    private fun setupVisibilityLogic() {
        // Ми "слухаємо" зміни екранів.
        // Якщо користувач перейшов на екран NotificationsFragment -> треба сховати меню і тулбар.
        // Якщо повернувся назад -> показати знову.
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

            if (currentFragment is NotificationsFragment) {
                bottomNav.visibility = View.GONE
                supportActionBar?.hide()
            } else {
                bottomNav.visibility = View.VISIBLE
                supportActionBar?.show()
            }
        }
    }

    private fun setupInitialFragment() {
        loadFragment(HomeFragment())
        bottomNav.selectedItemId = R.id.nav_home
    }

    // Універсальна функція для зміни екрану (фрагмента)
    private fun loadFragment(fragment: Fragment) {
        // Очищаємо історію "Назад", щоб не накопичувати купу вікон одна за одною
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // Підміняємо вміст контейнера на новий фрагмент
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // ДОЗВОЛИ (Permissions)
    private fun checkAndRequestPermissions() {
        // Тільки для нових Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                // Якщо дозволу немає - просимо користувача дозволити
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    // ОБРОБКА ПОСИЛАНЬ (Deep Links / Sharing)

    // Якщо додаток вже запущений і користувач кидає в нього посилання
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Оновлюємо поточний Intent
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        // Перевіряємо: чи це дія "Поділитися" (ACTION_SEND) і чи це текст (URL)
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text") == true) {
            val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return

            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

            if (currentFragment is HomeFragment) {
                // Якщо ми вже на Головній - просто вставляємо текст у поле
                currentFragment.updateUrl(sharedUrl)
            } else {
                // Якщо ми десь інде - відкриваємо Головну і передаємо туди посилання
                val homeFragment = HomeFragment().apply {
                    arguments = Bundle().apply { putString("sharedUrl", sharedUrl) }
                }
                loadFragment(homeFragment)
                bottomNav.selectedItemId = R.id.nav_home
            }

            Toast.makeText(this, "Посилання отримано!", Toast.LENGTH_SHORT).show()
        }
    }
}