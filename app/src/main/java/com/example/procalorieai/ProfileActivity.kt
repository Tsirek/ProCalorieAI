package com.procalorieai

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        TokenManager.init(this)

        setupNavigation()
        loadUserData()
        loadAchievements()
        loadGoal()
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val response = ApolloClientProvider.getApolloClient().query(GetMeQuery())
                    .execute()

                response.data?.me?.let { user ->
                    val firstName = user.username.split(" ").firstOrNull() ?: user.username
                    val lastName = user.username.split(" ").getOrNull(1) ?: ""

                    val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}"
                    findViewById<TextView>(R.id.tvInitials).text = initials.ifEmpty { user.username.take(2).uppercase() }
                    findViewById<TextView>(R.id.tvFullName).text = user.username
                    findViewById<TextView>(R.id.tvEmail).text = user.email
                    findViewById<TextView>(R.id.tvDaysCount).text = user.daysInApp.toString()
                    findViewById<TextView>(R.id.tvRecordsCount).text = user.totalRecords.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ProfileActivity, "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGoal() {
        lifecycleScope.launch {
            try {
                val response = ApolloClientProvider.getApolloClient().query(GetGoalQuery())
                    .execute()

                response.data?.goal?.let { goal ->
                    findViewById<TextView>(R.id.tvGoalDescription).text =
                        "${goal.goalType} на ${goal.targetKg.toInt()} кг"
                    findViewById<ProgressBar>(R.id.progressGoal).progress = goal.progressPercent.toInt()
                    findViewById<TextView>(R.id.tvGoalProgress).text =
                        if (goal.progressPercent >= 100) "✅ Цель достигнута!"
                        else "Выполнено на ${goal.progressPercent.toInt()}% • Осталось ${(goal.targetKg * (100 - goal.progressPercent) / 100).toInt()} кг"
                }
            } catch (e: Exception) {
                findViewById<TextView>(R.id.tvGoalDescription).text = "Цель не задана"
                findViewById<ProgressBar>(R.id.progressGoal).progress = 0
                findViewById<TextView>(R.id.tvGoalProgress).text = "Задайте цель в настройках"
            }
        }
    }

    private fun loadAchievements() {
        lifecycleScope.launch {
            try {
                val response = ApolloClientProvider.getApolloClient().query(GetAchievementsQuery())
                    .execute()

                response.data?.achievements?.let { ach ->
                    findViewById<TextView>(R.id.tvStreakDays).text = "${ach.streakDays} дней"
                    findViewById<TextView>(R.id.tvGoalsDone).text = if (ach.goalCreated) "Цель ✓" else "Цель"
                    findViewById<TextView>(R.id.tvPhotosCount).text = "${ach.photosAdded} фото"
                    val months = ach.daysInApp / 30
                    findViewById<TextView>(R.id.tvTotalDays).text = if (months >= 1) "${months} мес." else "${ach.daysInApp} дн."
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.btnCamera1).setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnProfile).setOnClickListener {
            Toast.makeText(this, "Вы уже в профиле", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.btnGoalsSettings).setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnPrivacy).setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, AssistantActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnGeneralSettings).setOnClickListener {
            startActivity(Intent(this, GeneralSettingsActivity::class.java))
        }
    }
}