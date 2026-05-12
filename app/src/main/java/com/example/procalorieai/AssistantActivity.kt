package com.procalorieai

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AssistantActivity : AppCompatActivity() {

    // Подготовленные вопросы и ответы
    private val faq = listOf(
        "Что такое ProCalorieAI?" to
                "ProCalorieAI — приложение для подсчёта калорий с помощью искусственного интеллекта. Вы фотографируете еду, AI распознаёт блюдо и автоматически рассчитывает КБЖУ. Также поддерживается ручной ввод.",

        "Как работает распознавание еды?" to
                "После съёмки фото нейросеть анализирует изображение, определяет блюдо и оценивает размер порции. Затем из базы данных извлекаются данные по КБЖУ. Точность зависит от качества фото и освещения.",

        "Правильно ли считаются калории?" to
                "Мы стремимся к точности, но автоматический расчёт — это оценка. Погрешность составляет ±10–15%. Для максимальной точности рекомендуем проверять данные вручную и при необходимости корректировать порции.",

        "Как достичь цели по похудению?" to
                "Задайте цель во вкладке Настройки → Цели. Приложение рассчитает рекомендованный дефицит калорий. Придерживайтесь нормы и регулярно фиксируйте приёмы пищи — прогресс будет виден в профиле.",

        "Как связаться с поддержкой?" to
                "Напишите нам: support@procalorieai.com\nМы отвечаем в течение 24 часов в рабочие дни. Также вы можете написать на support@procalorieai.com с темой письма «Обратная связь»."
    )

    // История диалога: Pair<isUser, text>
    private val messages = mutableListOf<Pair<Boolean, String>>()
    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistant)

        messagesContainer = findViewById(R.id.messagesContainer)
        scrollView        = findViewById(R.id.scrollViewChat)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Приветственное сообщение робота
        addBotMessage("Привет! 👋 Я AI-помощник ProCalorieAI. Выберите вопрос, чтобы получить ответ.")

        // Кнопки вопросов
        val questionsContainer = findViewById<LinearLayout>(R.id.questionsContainer)
        faq.forEachIndexed { index, (question, _) ->
            val btn = Button(this).apply {
                text = question
                textSize = 13f
                setPadding(24, 16, 24, 16)
                setBackgroundResource(R.drawable.bg_button_outline)
                setTextColor(getColor(R.color.text_dark))
                stateListAnimator = null
                // Преобразуем текст в нормальный регистр (не капс)
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 12) }
            }
            btn.setOnClickListener {
                handleQuestion(index)
            }
            questionsContainer.addView(btn)
        }
    }

    private fun handleQuestion(index: Int) {
        val (question, answer) = faq[index]

        // Сообщение пользователя
        addUserMessage(question)

        // Ответ бота с небольшой задержкой
        scrollView.postDelayed({
            addBotMessage(answer)
        }, 400)
    }

    private fun addUserMessage(text: String) {
        val view = layoutInflater.inflate(R.layout.item_chat_user, messagesContainer, false)
        view.findViewById<TextView>(R.id.tvMessage).text = text
        messagesContainer.addView(view)
        scrollToBottom()
    }

    private fun addBotMessage(text: String) {
        val view = layoutInflater.inflate(R.layout.item_chat_bot, messagesContainer, false)
        view.findViewById<TextView>(R.id.tvMessage).text = text
        messagesContainer.addView(view)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}