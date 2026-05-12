package com.procalorieai

import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PrivacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val prefs = getSharedPreferences("privacy_prefs", MODE_PRIVATE)

        val switchAnalytics  = findViewById<Switch>(R.id.switchAnalytics)
        val switchCrash      = findViewById<Switch>(R.id.switchCrashReports)
        val switchPersonal   = findViewById<Switch>(R.id.switchPersonalization)

        switchAnalytics.isChecked  = prefs.getBoolean("analytics", true)
        switchCrash.isChecked      = prefs.getBoolean("crash", true)
        switchPersonal.isChecked   = prefs.getBoolean("personal", true)

        switchAnalytics.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("analytics", checked).apply()
        }
        switchCrash.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("crash", checked).apply()
        }
        switchPersonal.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("personal", checked).apply()
        }

        findViewById<android.widget.LinearLayout>(R.id.btnDeleteData).setOnClickListener {
            Toast.makeText(this, "Запрос на удаление данных отправлен", Toast.LENGTH_SHORT).show()
        }
    }
}