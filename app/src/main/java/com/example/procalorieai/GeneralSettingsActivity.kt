package com.procalorieai

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.procalorieai.data.MealStore

class GeneralSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_general_settings)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val prefs = getSharedPreferences("general_prefs", MODE_PRIVATE)

        //val switchMetric   = findViewById<Switch>(R.id.switchMetricSystem)

       // switchMetric.isChecked   = prefs.getBoolean("metric", true)

      //  switchMetric.setOnCheckedChangeListener { _, checked ->
      //      prefs.edit().putBoolean("metric", checked).apply()
       // }

        // Очистить историю
        findViewById<LinearLayout>(R.id.btnClearHistory).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Очистить историю?")
                .setMessage("Все записи о питании будут удалены. Это действие нельзя отменить.")
                .setPositiveButton("Удалить") { _, _ ->
                    val meals = MealStore.meals.value?.toList() ?: emptyList()
                    meals.forEach { MealStore.delete(it) }
                    Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        // Выход из аккаунта
        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Выйти из аккаунта?")
                .setMessage("Вы будете перенаправлены на экран входа.")
                .setPositiveButton("Выйти") { _, _ ->
                    getSharedPreferences("procalorie_prefs", MODE_PRIVATE)
                        .edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}