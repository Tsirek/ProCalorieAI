package com.procalorieai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Onboarding2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding2)

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            startActivity(Intent(this, Onboarding3Activity::class.java))
        }

        findViewById<TextView>(R.id.tvSkip).setOnClickListener {
            skipToLogin()
        }
    }

    private fun skipToLogin() {
        getSharedPreferences("procalorie_prefs", MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}