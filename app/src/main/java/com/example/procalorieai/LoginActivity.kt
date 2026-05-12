package com.procalorieai

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.procalorieai.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import com.procalorieai.type.LoginInput


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TokenManager.init(this)

        // Показать/скрыть пароль
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = null
                binding.ivTogglePassword.alpha = 1f
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
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
                else -> performLogin(email, password)
            }
        }

        // Войти через Google
        binding.btnGoogle.setOnClickListener {
            showToast("Google авторизация будет доступна позже")
        }

        // Забыли пароль
        binding.tvForgotPassword.setOnClickListener {
            showToast("Восстановление пароля будет доступно позже")
        }

        // Перейти к регистрации
        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun performLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                showToast("Выполняется вход...")
                val response = ApolloClientProvider.getApolloClient().mutation(
                    LoginMutation(
                        input = LoginInput(
                            email = email,
                            password = password
                        )
                    )
                ).execute()

                val loginData = response.data?.login
                if (loginData != null) {
                    // Сохраняем токены
                    TokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)
                    ApolloClientProvider.refreshClient()

                    showToast("Вход выполнен!")
                    startActivity(Intent(this@LoginActivity, ProfileActivity::class.java))
                    finish()
                } else {
                    showToast("Неверный email или пароль")
                }
            } catch (e: Exception) {
                showToast("Ошибка: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}