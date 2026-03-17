package com.procalorieai

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.procalorieai.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Показать/скрыть пароль
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = null
                binding.ivTogglePassword.alpha = 1f
            } else {
                binding.etPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.ivTogglePassword.alpha = 0.5f
            }
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }

        // Кнопка Войти
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            when {
                email.isEmpty() -> showToast("Введите email")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    showToast("Некорректный email")
                password.isEmpty() -> showToast("Введите пароль")
                password.length < 6 -> showToast("Пароль слишком короткий")
                else -> {
                    // TODO: Реальная аутентификация
                    showToast("Выполняется вход...")
                }
            }
        }

        // Войти через Google
        binding.btnGoogle.setOnClickListener {
            showToast("Google авторизация")
        }

        // Забыли пароль
        binding.tvForgotPassword.setOnClickListener {
            showToast("Восстановление пароля")
        }

        // Перейти к регистрации
        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}