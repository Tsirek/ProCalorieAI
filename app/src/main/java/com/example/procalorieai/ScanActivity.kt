// ScanActivity.kt — реальный флоу загрузки фото
package com.procalorieai

import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import com.procalorieai.type.AddMealLogInput

class ScanActivity : AppCompatActivity() {

    private var currentPhotoUri: Uri? = null
    private lateinit var tvProteinValue: TextView
    private lateinit var tvCarbsValue: TextView
    private lateinit var tvFatValue: TextView
    private var progressBar: ProgressBar? = null
    private val httpClient = OkHttpClient()

    // Типы приёма пищи: UI → enum для бэкенда
    private val mealTypeMap = mapOf(
        "Завтрак" to "breakfast",
        "Обед"    to "lunch",
        "Ужин"    to "dinner",
        "Перекус" to "snack"
    )
    private val mealTypeLabels = mealTypeMap.keys.toTypedArray()

    private data class RecognizedMeal(
        val name: String,
        val calories: Int,
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        val mealType: String,   // русское название для UI
        val source: String
    )

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) currentPhotoUri?.let { uploadPhotoAndAnalyze(it) }
        else Toast.makeText(this, "Фото не сделано", Toast.LENGTH_SHORT).show()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadPhotoAndAnalyze(it) }
            ?: Toast.makeText(this, "Фото не выбрано", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_scan)
        TokenManager.init(this)
        initViews()
        setupButtons()
        checkPermissions()
    }

    private fun initViews() {
        tvProteinValue = findViewById(R.id.tvProteinValue)
        tvCarbsValue   = findViewById(R.id.tvCarbsValue)
        tvFatValue     = findViewById(R.id.tvFatValue)
        progressBar    = findViewById(R.id.progressBar)  // null если нет в layout — ок

        tvProteinValue.text = "—"
        tvCarbsValue.text   = "—"
        tvFatValue.text     = "—"
    }

    private fun setupButtons() {
        findViewById<View>(R.id.btnOpenCamera).setOnClickListener { openCamera() }
        findViewById<View>(R.id.btnFromGallery).setOnClickListener { openGallery() }
        findViewById<View>(R.id.btnManualInput).setOnClickListener { showManualInputSheet() }
    }

    // ── Шаг 1+2: запрашиваем presigned URL → загружаем фото → подтверждаем ──





    // ── Подтверждение и сохранение ──

    private fun showConfirmationCard(meal: RecognizedMeal) {
        val container = findViewById<ViewGroup>(R.id.confirmationContainer)
        val card      = findViewById<View>(R.id.confirmationCard)

        card.findViewById<TextView>(R.id.tvConfirmName).text     = meal.name
        card.findViewById<TextView>(R.id.tvConfirmCalories).text = "${meal.calories} ккал"
        card.findViewById<TextView>(R.id.tvConfirmProtein).text  = "${meal.protein.toInt()}г"
        card.findViewById<TextView>(R.id.tvConfirmCarbs).text    = "${meal.carbs.toInt()}г"
        card.findViewById<TextView>(R.id.tvConfirmFat).text      = "${meal.fat.toInt()}г"

        val spinner = card.findViewById<Spinner>(R.id.spinnerMealType)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mealTypeLabels)
        spinner.setSelection(mealTypeLabels.indexOf(meal.mealType).coerceAtLeast(0))

        card.findViewById<View>(R.id.btnConfirmSave).setOnClickListener {
            val selectedLabel = mealTypeLabels[spinner.selectedItemPosition]
            saveMealToServer(
                name      = meal.name,
                calories  = meal.calories,
                protein   = meal.protein,
                carbs     = meal.carbs,
                fat       = meal.fat,
                mealType  = mealTypeMap[selectedLabel] ?: "snack", // ← enum для бэка
                source    = meal.source
            )
            container.visibility = View.GONE
        }

        card.findViewById<View>(R.id.btnConfirmCancel).setOnClickListener {
            container.visibility = View.GONE
        }

        container.visibility = View.VISIBLE
    }

    private fun saveMealToServer(
        name: String, calories: Int,
        protein: Float, carbs: Float, fat: Float,
        mealType: String, source: String
    ) {
        lifecycleScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .format(Date())

                val response = ApolloClientProvider.getApolloClient()
                    .mutation(
                        AddMealMutation(
                            input = AddMealLogInput(
                                name     = name,
                                mealType = mealType,   // "breakfast" / "lunch" / "dinner" / "snack"
                                source   = source,
                                eatenAt  = timestamp,
                                calories = calories,
                                protein  = protein.toDouble(),
                                carbs    = carbs.toDouble(),
                                fat      = fat.toDouble()
                            )
                        )
                    ).execute()

                if (response.data?.addMealLog != null) {
                    tvProteinValue.text = "${protein.toInt()}г"
                    tvCarbsValue.text   = "${carbs.toInt()}г"
                    tvFatValue.text     = "${fat.toInt()}г"
                    Toast.makeText(this@ScanActivity, "✅ Добавлено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ScanActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScanActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Ручной ввод ──

    private fun showManualInputSheet(defaultSource: String = "manual") {
        val sheet     = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_manual_input, null)
        sheet.setContentView(sheetView)

        val etName     = sheetView.findViewById<EditText>(R.id.etMealName)
        val etCalories = sheetView.findViewById<EditText>(R.id.etCalories)
        val etProtein  = sheetView.findViewById<EditText>(R.id.etProtein)
        val etCarbs    = sheetView.findViewById<EditText>(R.id.etCarbs)
        val etFat      = sheetView.findViewById<EditText>(R.id.etFat)
        val spinner    = sheetView.findViewById<Spinner>(R.id.spinnerMealType)
        val btnSave    = sheetView.findViewById<View>(R.id.btnSaveManual)

        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mealTypeLabels)

        btnSave.setOnClickListener {
            val name    = etName.text.toString().trim()
            val cal     = etCalories.text.toString().toIntOrNull()
            val protein = etProtein.text.toString().toFloatOrNull() ?: 0f
            val carbs   = etCarbs.text.toString().toFloatOrNull()   ?: 0f
            val fat     = etFat.text.toString().toFloatOrNull()     ?: 0f

            if (name.isEmpty())        { etName.error = "Введите название"; return@setOnClickListener }
            if (cal == null || cal <= 0) { etCalories.error = "Введите калории"; return@setOnClickListener }

            showConfirmationCard(
                RecognizedMeal(
                    name     = name,
                    calories = cal,
                    protein  = protein,
                    carbs    = carbs,
                    fat      = fat,
                    mealType = mealTypeLabels[spinner.selectedItemPosition],
                    source   = defaultSource
                )
            )
            sheet.dismiss()
        }
        sheet.show()
    }

    // ── Утилиты ──

    private fun setLoading(loading: Boolean) {
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE  // ?. не крашит
        findViewById<View>(R.id.btnOpenCamera).isEnabled  = !loading
        findViewById<View>(R.id.btnFromGallery).isEnabled = !loading
        findViewById<View>(R.id.btnManualInput).isEnabled = !loading
    }
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) { checkPermissions(); return }

        val cv = ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProCalorieAI")
        }
        currentPhotoUri = contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv
        )
        currentPhotoUri?.let { takePictureLauncher.launch(it) }
    }

    // ScanActivity.kt — метод uploadPhotoAndAnalyze с логами
    private fun uploadPhotoAndAnalyze(imageUri: Uri) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                Log.d(TAG, "=== Начинаем загрузку фото ===")
                Log.d(TAG, "URI: $imageUri")

                // Шаг 1 — получаем presigned URL
                Log.d(TAG, "Шаг 1: запрашиваем presigned URL у бэкенда...")
                val uploadData = ApolloClientProvider.getApolloClient()
                    .mutation(GetUploadUrlMutation())
                    .execute()
                    .also { Log.d(TAG, "Ответ бэкенда: errors=${it.errors}, data=${it.data}") }
                    .data?.requestPhotoUpload
                    ?: throw Exception("Бэкенд не вернул URL для загрузки")

                Log.d(TAG, "Получили presigned URL: ${uploadData.uploadUrl}")
                Log.d(TAG, "s3Key: ${uploadData.s3Key}")
                Log.d(TAG, "requestId: ${uploadData.requestId}")

                // Шаг 2 — читаем байты фото
                Log.d(TAG, "Шаг 2: читаем байты фото...")
                val photoBytes = readUriBytes(imageUri)
                    ?: throw Exception("Не удалось прочитать фото по URI: $imageUri")
                Log.d(TAG, "Фото прочитано: ${photoBytes.size} байт")

                // Шаг 3 — загружаем в S3
                Log.d(TAG, "Шаг 3: загружаем в S3 по URL: ${uploadData.uploadUrl}")
                val uploaded = uploadToS3(uploadData.uploadUrl, photoBytes)
                Log.d(TAG, "Результат загрузки в S3: $uploaded")

                if (!uploaded) throw Exception("S3 вернул ошибку при загрузке")

                // Шаг 4 — подтверждаем
                Log.d(TAG, "Шаг 4: подтверждаем загрузку (requestId=${uploadData.requestId})...")
                val confirmResponse = ApolloClientProvider.getApolloClient()
                    .mutation(ConfirmPhotoUploadMutation(requestId = uploadData.requestId))
                    .execute()
                Log.d(TAG, "Подтверждение: errors=${confirmResponse.errors}, status=${confirmResponse.data?.confirmPhotoUpload?.status}")

                Log.d(TAG, "=== Загрузка завершена успешно ===")
                Toast.makeText(this@ScanActivity, "Фото отправлено на анализ!", Toast.LENGTH_LONG).show()
                showManualInputSheet(defaultSource = "camera")

            } catch (e: Exception) {
                Log.e(TAG, "=== ОШИБКА при загрузке ===", e)
                Log.e(TAG, "Тип: ${e.javaClass.simpleName}")
                Log.e(TAG, "Сообщение: ${e.message}")
                Toast.makeText(this@ScanActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    // uploadToS3 — с детальными логами
    private suspend fun uploadToS3(presignedUrl: String, bytes: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "PUT → $presignedUrl")
                Log.d(TAG, "Размер: ${bytes.size} байт")

                val request = Request.Builder()
                    .url(presignedUrl)
                    .put(bytes.toRequestBody("image/jpeg".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()

                Log.d(TAG, "S3 ответ: code=${response.code} message=${response.message}")
                Log.d(TAG, "S3 headers: ${response.headers}")

                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: "нет тела"
                    Log.e(TAG, "S3 ошибка body: $body")
                }

                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "uploadToS3 exception: ${e.javaClass.simpleName}: ${e.message}", e)
                false
            }
        }

    companion object {
        private const val TAG = "ScanActivity"
    }

    private fun openGallery() { pickImageLauncher.launch("image/*") }

    private fun checkPermissions() {
        val perms = buildList {
            if (ContextCompat.checkSelfPermission(this@ScanActivity, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) add(android.Manifest.permission.CAMERA)
            val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                android.Manifest.permission.READ_MEDIA_IMAGES
            else android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this@ScanActivity, mediaPermission)
                != PackageManager.PERMISSION_GRANTED) add(mediaPermission)
        }
        if (perms.isNotEmpty()) requestPermissionsLauncher.launch(perms.toTypedArray())
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it })
            Toast.makeText(this, "Нужны разрешения для камеры и галереи", Toast.LENGTH_LONG).show()
    }

    private suspend fun readUriBytes(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) { null }
    }
}




