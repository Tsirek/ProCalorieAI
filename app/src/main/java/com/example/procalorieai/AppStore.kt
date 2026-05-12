package com.procalorieai.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Calendar

object AppStore {

    // ── Цель пользователя ──────────────────────────────────────────────────

    data class UserGoal(
        val type: String = "Похудение",       // "Похудение" / "Поддержание" / "Набор"
        val targetKg: Float = 5f,             // сколько кг сбросить/набрать
        val currentWeightKg: Float = 75f,
        val targetWeightKg: Float = 70f,
        val dailyCalorieTarget: Int = 1800,   // рассчитанная цель по ккал
        val progressKg: Float = 0f,           // уже достигнуто
        val isSet: Boolean = false
    )

    private val _goal = MutableLiveData<UserGoal>(UserGoal())
    val goal: LiveData<UserGoal> = _goal

    fun setGoal(g: UserGoal) { _goal.value = g }

    fun getGoalProgress(): Int {
        val g = _goal.value ?: return 0
        if (!g.isSet || g.targetKg == 0f) return 0
        // Считаем прогресс по дефициту калорий: 7700 ккал ≈ 1 кг жира
        val totalDeficit = calculateTotalDeficit()
        val kgLost = (totalDeficit / 7700f).coerceAtLeast(0f)
        val progress = ((kgLost / g.targetKg) * 100).toInt().coerceIn(0, 100)
        _goal.value = g.copy(progressKg = kgLost)
        return progress
    }

    // Суммарный дефицит: (целевые ккал - фактические) * дни
    private fun calculateTotalDeficit(): Float {
        val g = _goal.value ?: return 0f
        val avg = MealStore.weeklyAvg()
        if (avg == 0f) return 0f
        val dailyDeficit = g.dailyCalorieTarget - avg
        // считаем за 7 дней как заглушка
        return (dailyDeficit * 7f).coerceAtLeast(0f)
    }

    // ── Настройки пользователя ─────────────────────────────────────────────

    data class UserPreferences(
        val dietType: String = "Без ограничений",  // "Вегетарианец", "Веган", "Без глютена"
        val allergies: List<String> = emptyList(),
        val mealsPerDay: Int = 3,
        val waterGoalLiters: Float = 2f,
        val notificationsEnabled: Boolean = true,
        val reminderHour: Int = 12
    )

    private val _prefs = MutableLiveData<UserPreferences>(UserPreferences())
    val userPrefs: LiveData<UserPreferences> = _prefs
    fun setPreferences(p: UserPreferences) { _prefs.value = p }

    // ── Достижения ─────────────────────────────────────────────────────────

    data class Achievements(
        val streakDays: Int = 0,
        val goalCreated: Boolean = false,
        val photosAdded: Int = 0,
        val daysInApp: Int = 1
    )

    private val _achievements = MutableLiveData<Achievements>(Achievements())
    val achievements: LiveData<Achievements> = _achievements

    fun recalcAchievements() {
        val meals = MealStore.meals.value ?: return
        val photos = meals.count { it.source == "camera" }
        val goalCreated = _goal.value?.isSet ?: false
        val days = calculateDaysInApp()
        _achievements.value = Achievements(
            streakDays    = calculateStreak(),
            goalCreated   = goalCreated,
            photosAdded   = photos,
            daysInApp     = days
        )
    }

    private fun calculateStreak(): Int {
        val meals = MealStore.meals.value ?: return 0
        if (meals.isEmpty()) return 0
        var streak = 0
        var day = startOfToday()
        while (true) {
            val end = day + 86_400_000L
            val hasMeal = meals.any { it.timestamp in day until end }
            if (hasMeal) { streak++; day -= 86_400_000L } else break
        }
        return streak
    }

    private fun calculateDaysInApp(): Int {
        // Заглушка — в реальном проекте берётся из даты регистрации
        return 42
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}