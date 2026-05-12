package com.procalorieai.data

// Простая модель приёма пищи — хранится в памяти до подключения бэкенда
data class MealEntry(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val mealType: String,   // "Завтрак" / "Обед" / "Ужин" / "Перекус"
    val source: String,     // "manual" / "camera"
    val timestamp: Long = System.currentTimeMillis()
)