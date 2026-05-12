package com.procalorieai.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Calendar

// Хранилище в памяти — заменить на репозиторий с бэкендом
object MealStore {

    private val _meals = MutableLiveData<MutableList<MealEntry>>(mutableListOf(
        // Моковые данные для демонстрации
        MealEntry(
            name = "Куриная грудка с рисом",
            calories = 520,
            protein = 45f,
            carbs = 52f,
            fat = 12f,
            mealType = "Обед",
            source = "manual",
            timestamp = todayAt(14, 30)
        ),
        MealEntry(
            name = "Овсянка с бананом",
            calories = 380,
            protein = 12f,
            carbs = 68f,
            fat = 6f,
            mealType = "Завтрак",
            source = "manual",
            timestamp = todayAt(9, 15)
        )
    ))

    val meals: LiveData<MutableList<MealEntry>> = _meals

    fun add(meal: MealEntry) {
        val list = _meals.value ?: mutableListOf()
        list.add(0, meal)
        _meals.value = list
    }

    fun delete(meal: MealEntry) {
        val list = _meals.value ?: return
        list.removeAll { it.id == meal.id }
        _meals.value = list
    }

    fun todayMeals(): List<MealEntry> {
        val start = startOfToday()
        val end   = start + 86_400_000L
        return _meals.value?.filter { it.timestamp in start until end } ?: emptyList()
    }

    fun todayCalories(): Int = todayMeals().sumOf { it.calories }

    fun weeklyAvg(): Float {
        val weekAgo = startOfToday() - 7 * 86_400_000L
        val recent  = _meals.value?.filter { it.timestamp >= weekAgo } ?: return 0f
        if (recent.isEmpty()) return 0f
        // группируем по дням
        val byDay = recent.groupBy { dayKey(it.timestamp) }
        return byDay.values.map { it.sumOf { m -> m.calories } }.average().toFloat()
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun todayAt(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour); cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0);          cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun dayKey(ts: Long): String {
        val cal = Calendar.getInstance(); cal.timeInMillis = ts
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }
}