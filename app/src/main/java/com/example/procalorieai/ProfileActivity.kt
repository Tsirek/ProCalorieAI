package com.procalorieai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ProfileActivity : AppCompatActivity() {

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(this, "Нет доступа к камере", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setupUserData()
        setupNavigation()
    }

    private fun setupUserData() {
        // Данные пользователя — в реальном проекте берутся из SharedPreferences / ViewModel
        val firstName = "Анна"
        val lastName = "Васильева"
        val email = "anna.v@example.com"
        val daysCount = 42
        val recordsCount = 156
        val weightChange = -3.5
        val goalDescription = "Похудеть на 5 кг"
        val goalProgress = 70
        val goalRemaining = 1.5
        val streakDays = 7
        val goalsDone = 3
        val photosCount = 100
        val totalDays = 42

        // Initials
        val initials = "${firstName.first()}${lastName.first()}"
        findViewById<TextView>(R.id.tvInitials).text = initials

        // Name & Email
        findViewById<TextView>(R.id.tvFullName).text = "$firstName $lastName"
        findViewById<TextView>(R.id.tvEmail).text = email

        // Stats
        findViewById<TextView>(R.id.tvDaysCount).text = daysCount.toString()
        findViewById<TextView>(R.id.tvRecordsCount).text = recordsCount.toString()
        findViewById<TextView>(R.id.tvWeightChange).text = weightChange.toString()

        // Goal
        findViewById<TextView>(R.id.tvGoalDescription).text = goalDescription
        findViewById<ProgressBar>(R.id.progressGoal).progress = goalProgress
        findViewById<TextView>(R.id.tvGoalProgress).text =
            "Выполнено на $goalProgress% • Осталось $goalRemaining кг"

        // Achievements
        findViewById<TextView>(R.id.tvStreakDays).text = "$streakDays дней"
        findViewById<TextView>(R.id.tvGoalsDone).text =
            if (goalsDone == 1) "Цель" else "$goalsDone цели"
        findViewById<TextView>(R.id.tvPhotosCount).text = "$photosCount фото"
        val months = totalDays / 30
        findViewById<TextView>(R.id.tvTotalDays).text =
            if (months >= 1) "${months} мес." else "$totalDays дн."
    }

    private fun setupNavigation() {
        // Camera
        findViewById<LinearLayout>(R.id.btnCamera).setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED -> openCamera()
                else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        // History
        findViewById<LinearLayout>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Profile — уже на этой странице
        findViewById<LinearLayout>(R.id.btnProfile).setOnClickListener {
            // Already here
        }

        // Settings items
        findViewById<LinearLayout>(R.id.btnGoalsSettings).setOnClickListener {
            Toast.makeText(this, "Цели и предпочтения", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.btnNotifications).setOnClickListener {
            Toast.makeText(this, "Уведомления", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.btnProSubscription).setOnClickListener {
            Toast.makeText(this, "Подписка Pro", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.btnPrivacy).setOnClickListener {
            Toast.makeText(this, "Конфиденциальность", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.btnHelp).setOnClickListener {
            Toast.makeText(this, "Помощь и поддержка", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.btnGeneralSettings).setOnClickListener {
            Toast.makeText(this, "Общие настройки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show()
        }
    }
}