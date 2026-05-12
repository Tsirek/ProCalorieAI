// PreferencesActivity.kt — подключаем бэкенд вместо AppStore
package com.procalorieai

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.procalorieai.type.PreferencesInput
import com.apollographql.apollo3.api.Optional

class PreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        TokenManager.init(this)

        val spinnerDiet  = findViewById<Spinner>(R.id.spinnerDiet)
        val spinnerMeals = findViewById<Spinner>(R.id.spinnerMealsPerDay)
        val etWater      = findViewById<EditText>(R.id.etWaterGoal)
        val switchNotify = findViewById<Switch>(R.id.switchNotifications)
        val btnSave      = findViewById<Button>(R.id.btnSavePreferences)
        val btnBack      = findViewById<ImageView>(R.id.btnBack)

        val dietTypes = arrayOf(
            "balanced", "vegetarian", "vegan",
            "gluten_free", "lactose_free", "keto", "paleo"
        )
        val dietLabels = arrayOf(
            "Без ограничений", "Вегетарианец", "Веган",
            "Без глютена", "Без лактозы", "Кето", "Палео"
        )
        spinnerDiet.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, dietLabels
        )

        val mealsOptions = arrayOf("2 раза", "3 раза", "4 раза", "5 раз")
        spinnerMeals.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, mealsOptions
        )

        // Загружаем текущие настройки с бэкенда
        lifecycleScope.launch {
            try {
                val response = ApolloClientProvider.getApolloClient()
                    .query(GetPreferencesQuery()).execute()

                response.data?.preferences?.let { prefs ->
                    val dietIdx = dietTypes.indexOf(prefs.dietType).coerceAtLeast(0)
                    spinnerDiet.setSelection(dietIdx)
                    spinnerMeals.setSelection((prefs.mealsPerDay - 2).coerceIn(0, 3))
                    // waterGoalMl → литры для UI
                    etWater.setText((prefs.waterGoalMl / 1000f).toString())
                    switchNotify.isChecked = prefs.notificationsEnabled
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // PreferencesActivity.kt — только блок сохранения
        btnSave.setOnClickListener {
            val waterLiters = etWater.text.toString().toFloatOrNull() ?: 2f
            val waterMl     = (waterLiters * 1000).toInt()
            val mealsPerDay = spinnerMeals.selectedItemPosition + 2
            val dietType    = dietTypes[spinnerDiet.selectedItemPosition]

            lifecycleScope.launch {
                try {
                    val response = ApolloClientProvider.getApolloClient()
                        .mutation(
                            SavePreferencesMutation(
                                input = com.procalorieai.type.PreferencesInput(
                                    dietType             = dietType,
                                    mealsPerDay          = mealsPerDay,
                                    waterGoalMl          = waterMl,
                                    notificationsEnabled = switchNotify.isChecked
                                )
                            )
                        ).execute()

                    if (response.data?.updatePreferences != null) {
                        Toast.makeText(this@PreferencesActivity, "✅ Сохранено", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@PreferencesActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@PreferencesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnBack.setOnClickListener { finish() }
    }
}