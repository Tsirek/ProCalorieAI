package com.procalorieai

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.procalorieai.type.GoalInput

class GoalsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        TokenManager.init(this)

        val spinnerGoalType = findViewById<Spinner>(R.id.spinnerGoalType)
        val etCurrentWeight = findViewById<EditText>(R.id.etCurrentWeight)
        val etTargetKg = findViewById<EditText>(R.id.etTargetKg)
        val tvCalcResult = findViewById<TextView>(R.id.tvCalcResult)
        val tvAvgCalories = findViewById<TextView>(R.id.tvAvgCalories)
        val tvDeficitSurplus = findViewById<TextView>(R.id.tvDeficitSurplus)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val btnSaveGoal = findViewById<Button>(R.id.btnSaveGoal)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        val goalTypes = arrayOf("Похудение", "Поддержание веса", "Набор массы")
        spinnerGoalType.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, goalTypes
        )

        loadCurrentGoal(spinnerGoalType, etCurrentWeight, etTargetKg, goalTypes, tvAvgCalories)

        btnCalculate.setOnClickListener {
            calculateGoal(
                spinnerGoalType, etCurrentWeight, etTargetKg,
                tvCalcResult, tvDeficitSurplus, goalTypes
            )
        }

        btnSaveGoal.setOnClickListener {
            saveGoal(
                spinnerGoalType, etCurrentWeight, etTargetKg,
                tvCalcResult, goalTypes
            )
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun mapGoalTypeToRu(enGoalType: String): String {
        return when (enGoalType) {
            "loss" -> "Похудение"
            "maintain" -> "Поддержание веса"
            "gain" -> "Набор массы"
            else -> "Поддержание веса"
        }
    }


    private fun loadCurrentGoal(
        spinnerGoalType: Spinner,
        etCurrentWeight: EditText,
        etTargetKg: EditText,
        goalTypes: Array<String>,
        tvAvgCalories: TextView
    ) {
        lifecycleScope.launch {
            try {
                val response = ApolloClientProvider.getApolloClient().query(GetGoalQuery())
                    .execute()

                response.data?.goal?.let { goal ->
                    if (goal.id != null) {
                        etCurrentWeight.setText(goal.currentWeightKg.toInt().toString())
                        etTargetKg.setText(goal.targetKg.toInt().toString())
                        val idx = goalTypes.indexOf(mapGoalTypeToRu(goal.goalType)).coerceAtLeast(0)
                        spinnerGoalType.setSelection(idx)
                    }
                }

                // Загружаем среднюю калорийность
                val mealsResponse = ApolloClientProvider.getApolloClient().query(GetMealLogsQuery())
                    .execute()
                val meals = mealsResponse.data?.meals.orEmpty()
                val avgCal = if (meals.isNotEmpty()) {
                    meals.sumOf { it.nutrition.calories.toInt() } / meals.size
                } else 0
                tvAvgCalories.text = if (avgCal > 0) "Ваша средняя: $avgCal ккал/день" else "История пуста — средняя не рассчитана"
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@GoalsActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Добавь эту функцию в класс GoalsActivity
    private fun mapGoalTypeToEn(russianGoalType: String): String {
        return when (russianGoalType) {
            "Похудение" -> "loss"
            "Поддержание веса" -> "maintain"
            "Набор массы" -> "gain"
            else -> "maintain" // fallback
        }
    }

    private fun calculateGoal(
        spinnerGoalType: Spinner,
        etCurrentWeight: EditText,
        etTargetKg: EditText,
        tvCalcResult: TextView,
        tvDeficitSurplus: TextView,
        goalTypes: Array<String>
    ) {
        val currentW = etCurrentWeight.text.toString().toDoubleOrNull()
        val targetKg = etTargetKg.text.toString().toDoubleOrNull()

        if (currentW == null || currentW <= 0.0) {
            etCurrentWeight.error = "Введите текущий вес"
            return
        }
        if (targetKg == null || targetKg <= 0.0) {
            etTargetKg.error = "Введите цель в кг"
            return
        }

        val goalType = goalTypes[spinnerGoalType.selectedItemPosition]
        val avg = 2000.0

        val dailyTarget = when (goalType) {
            "Похудение" -> ((avg - 500.0).coerceAtLeast(1200.0)).toInt()
            "Набор массы" -> (avg + 300.0).toInt()
            else -> avg.toInt()
        }

        val deficit = avg - dailyTarget
        val weeksNeeded = if (deficit > 0.0) (targetKg * 7700.0 / (deficit * 7.0)).toInt() else 0

        tvCalcResult.text = "🎯 Цель: $dailyTarget ккал/день"
        tvCalcResult.tag = dailyTarget

        tvDeficitSurplus.text = when {
            deficit > 0.0 -> "📉 Дефицит: ${deficit.toInt()} ккал/день\n⏱ Примерно $weeksNeeded нед. до цели"
            deficit < 0.0 -> "📈 Профицит: ${abs(deficit).toInt()} ккал/день"
            else -> "✅ Поддержание текущего веса"
        }
    }

    private fun saveGoal(
        spinnerGoalType: Spinner,
        etCurrentWeight: EditText,
        etTargetKg: EditText,
        tvCalcResult: TextView,
        goalTypes: Array<String>
    ) {
        val currentW = etCurrentWeight.text.toString().toDoubleOrNull() ?: 75.0
        val targetKg = etTargetKg.text.toString().toDoubleOrNull() ?: 5.0
        val dailyTarget = (tvCalcResult.tag as? Int) ?: 1800
        val russianGoal = goalTypes[spinnerGoalType.selectedItemPosition]
        val goalTypeEn = mapGoalTypeToEn(russianGoal)  // <- маппер здесь

        lifecycleScope.launch {
            try {
                val response = ApolloClientProvider.getApolloClient().mutation(
                    SetGoalMutation(
                        input = GoalInput(
                            goalType = goalTypeEn,           // теперь "loss"/"maintain"/"gain"
                            targetKg = targetKg,
                            currentWeightKg = currentW,
                            dailyCalories = dailyTarget
                        )
                    )
                ).execute()

                if (response.data?.upsertGoal != null) {
                    Toast.makeText(this@GoalsActivity, "✅ Цель сохранена!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@GoalsActivity, "Ошибка сохранения цели", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GoalsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}