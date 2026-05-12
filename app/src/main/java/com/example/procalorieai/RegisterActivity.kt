package com.procalorieai

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.procalorieai.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch
import com.procalorieai.type.RegisterInput


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: RegisterActivity создана")
        TokenManager.init(this)
        Log.d(TAG, "TokenManager инициализирован")

        // Показать/скрыть пароль
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = null
                binding.ivTogglePassword.alpha = 1f
                Log.d(TAG, "Пароль видимый")
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.ivTogglePassword.alpha = 0.5f
                Log.d(TAG, "Пароль скрыт")
            }
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }

        // Кнопка Создать аккаунт
        binding.btnRegister.setOnClickListener {
            Log.d(TAG, "Нажата кнопка регистрации")
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            Log.d(TAG, "Поля: name='$name', email='$email', password.length=${password.length}")

            when {
                name.isEmpty() -> {
                    Log.w(TAG, "Имя пустое")
                    showToast("Введите ваше имя")
                }
                name.length < 2 -> {
                    Log.w(TAG, "Имя слишком короткое: ${name.length}")
                    showToast("Имя слишком короткое")
                }
                email.isEmpty() -> {
                    Log.w(TAG, "Email пустой")
                    showToast("Введите email")
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Log.w(TAG, "Некорректный email: $email")
                    showToast("Некорректный email")
                }
                password.isEmpty() -> {
                    Log.w(TAG, "Пароль пустой")
                    showToast("Введите пароль")
                }
                password.length < 6 -> {
                    Log.w(TAG, "Пароль слишком короткий: ${password.length}")
                    showToast("Пароль должен быть минимум 6 символов")
                }
                else -> {
                    Log.d(TAG, "Валидация пройдена, переходим к регистрации")
                    performRegistration(name, email, password)
                }
            }
        }

        // Продолжить с Google
        binding.btnGoogle.setOnClickListener {
            Log.d(TAG, "Нажата кнопка Google регистрации")
            showToast("Google регистрация будет доступна позже")
        }

        // Перейти ко входу
        binding.tvLoginLink.setOnClickListener {
            Log.d(TAG, "Переход на LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun performRegistration(name: String, email: String, password: String) {
        Log.d(TAG, "performRegistration: начало регистрации")
        Log.d(TAG, "Данные: name='$name', email='$email'")

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Получаем ApolloClient...")
                val client = ApolloClientProvider.getApolloClient()

                Log.d(TAG, "Создаём RegisterInput с email: $email")
                val input = RegisterInput(
                    username = name,
                    email = email,
                    password = password
                )

                Log.d(TAG, "Отправляем RegisterMutation...")
                val response = client.mutation(
                    RegisterMutation(input = input)
                ).execute()

                Log.d(TAG, "Ответ получен!")
                Log.d(TAG, "hasErrors: ${response.hasErrors()}")
                Log.d(TAG, "data: ${response.data}")

                if (response.hasErrors()) {
                    response.errors?.forEach { error ->
                        Log.e(TAG, "GraphQL ошибка: ${error.message}")
                    }
                }

                val registerData = response.data?.register
                Log.d(TAG, "registerData = $registerData")

                if (registerData != null) {
                    Log.d(TAG, "Регистрация успешна!")
                    Log.d(TAG, "accessToken: ${registerData.accessToken.take(20)}...")
                    Log.d(TAG, "refreshToken: ${registerData.refreshToken.take(20)}...")

                    TokenManager.saveTokens(registerData.accessToken, registerData.refreshToken)
                    Log.d(TAG, "Токены сохранены")

                    ApolloClientProvider.refreshClient()
                    Log.d(TAG, "Клиент обновлён")

                    showToast("Аккаунт создан!")
                    Log.d(TAG, "Переход на ProfileActivity")
                    startActivity(Intent(this@RegisterActivity, ProfileActivity::class.java))
                    finish()
                } else {
                    Log.e(TAG, "registerData == null, регистрация не удалась")
                    if (!response.hasErrors()) {
                        Log.e(TAG, "Но errors тоже нет! Возможно проблема с сетевым подключением")
                    }
                    showToast("Ошибка регистрации")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при регистрации: ${e.javaClass.simpleName}")
                Log.e(TAG, "Сообщение: ${e.message}")
                Log.e(TAG, "Стек вызовов:", e)
                showToast("Ошибка: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Log.d(TAG, "Toast: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}