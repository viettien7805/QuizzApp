package com.tien.quizapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class CreateQuizActivity : AppCompatActivity() {

    // --- CẤU HÌNH ---
    private val geminiApiKey = "AIzaSyAKEglKOpYDZeHNJGFBxXzghx_Qf9o0Zf0"

    // URL Database
    private val databaseUrl = "https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = geminiApiKey
    )

    private val questionsList = mutableListOf<QuestionModel>()

    private lateinit var etTitle: EditText
    private lateinit var etTime: EditText
    private lateinit var etQuestion: EditText
    private lateinit var etA: EditText
    private lateinit var etB: EditText
    private lateinit var etC: EditText
    private lateinit var etD: EditText
    private lateinit var rgCorrect: RadioGroup
    private lateinit var tvCount: TextView
    private lateinit var progressBar: ProgressBar

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                processFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_quiz)

        initViews()

        findViewById<Button>(R.id.btnUploadFile).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            filePickerLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnAddQuestion).setOnClickListener {
            if (addManualQuestionToBuffer()) {
                resetManualInputs()
                updateQuestionCountUI()
                Toast.makeText(this, "Đã thêm thủ công!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnFinish).setOnClickListener {
            finishQuiz()
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etQuizTitle)
        etTime = findViewById(R.id.etQuizTime)
        etQuestion = findViewById(R.id.etQuestion)
        etA = findViewById(R.id.etOptionA)
        etB = findViewById(R.id.etOptionB)
        etC = findViewById(R.id.etOptionC)
        etD = findViewById(R.id.etOptionD)
        rgCorrect = findViewById(R.id.rgCorrectAnswer)
        tvCount = findViewById(R.id.tvQuestionCount)
        progressBar = findViewById(R.id.progressBar)
    }

    // --- PHẦN XỬ LÝ AI & FILE (ĐÃ SỬA PROMPT) ---

    private fun processFile(fileUri: Uri) {
        progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "Đang đọc PDF...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Đọc text
                val textContent = extractTextFromPdf(fileUri)

                if (textContent.length < 50) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateQuizActivity, "File PDF quá ngắn hoặc lỗi!", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateQuizActivity, "Đang nhờ AI tạo câu hỏi...", Toast.LENGTH_SHORT).show()
                }

                // 2. Gửi cho Gemini (Đã sửa prompt bên dưới)
                val generatedJson = generateQuestionsWithGemini(textContent)

                // 3. Xử lý kết quả
                if (generatedJson != null) {
                    val newQuestions = parseQuestionsFromJson(generatedJson)

                    // --- BỘ LỌC QUAN TRỌNG: Loại bỏ câu hỏi lỗi ---
                    val validQuestions = newQuestions.filter {
                        it.question.isNotEmpty() && it.options.size >= 2 && it.correct.isNotEmpty()
                    }
                    // ----------------------------------------------

                    withContext(Dispatchers.Main) {
                        if (validQuestions.isNotEmpty()) {
                            questionsList.addAll(validQuestions)
                            updateQuestionCountUI()
                            Toast.makeText(this@CreateQuizActivity, "Thành công! Đã thêm ${validQuestions.size} câu từ AI.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@CreateQuizActivity, "Lỗi: AI trả về dữ liệu không đúng định dạng.", Toast.LENGTH_LONG).show()
                            Log.e("AI_DEBUG", "JSON trả về: $generatedJson") // Xem log để debug
                        }
                        progressBar.visibility = View.GONE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateQuizActivity, "Lỗi kết nối AI.", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateQuizActivity, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    e.printStackTrace()
                }
            }
        }
    }

    // --- HÀM PROMPT MỚI (QUAN TRỌNG NHẤT) ---
    private suspend fun generateQuestionsWithGemini(text: String): String? {
        // Prompt này ép AI dùng đúng tên biến: "question", "options", "correct"
        val prompt = """
            Dựa vào văn bản sau, hãy tạo ra 5 câu hỏi trắc nghiệm tiếng Việt.
            YÊU CẦU BẮT BUỘC VỀ ĐỊNH DẠNG JSON:
            1. Trả về MỘT JSON ARRAY thuần túy.
            2. Tên các trường (Key) phải chính xác 100% như sau (viết thường): "question", "options", "correct".
            3. "options" là một mảng chứa 4 đáp án dạng chuỗi.
            4. "correct" là nội dung đáp án đúng (phải khớp y hệt một trong các chuỗi trong options).
            
            Ví dụ mẫu JSON:
            [
              {
                "question": "Thủ đô của Việt Nam là gì?",
                "options": ["Hà Nội", "Huế", "Đà Nẵng", "TP.HCM"],
                "correct": "Hà Nội"
              }
            ]
            
            Không thêm markdown (```json), chỉ trả về text JSON.
            
            Văn bản nguồn:
            $text
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            var cleanJson = response.text?.trim() ?: ""

            // Làm sạch JSON nếu AI lỡ thêm format markdown
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.removePrefix("```json").removeSuffix("```").trim()
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.removePrefix("```").removeSuffix("```").trim()
            }

            Log.d("AI_RESPONSE", cleanJson) // In ra Logcat để kiểm tra
            cleanJson
        } catch (e: Exception) {
            Log.e("GeminiError", e.message.toString())
            null
        }
    }

    private fun extractTextFromPdf(uri: Uri): String {
        val stringBuilder = StringBuilder()
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            val reader = PdfReader(inputStream)
            val n = reader.numberOfPages
            for (i in 0 until n) {
                val text = PdfTextExtractor.getTextFromPage(reader, i + 1)
                stringBuilder.append(text).append("\n")
                if (stringBuilder.length > 20000) break
            }
            reader.close()
        } catch (e: Exception) {
            return ""
        } finally {
            inputStream?.close()
        }
        return stringBuilder.toString().trim()
    }

    private fun parseQuestionsFromJson(jsonString: String): List<QuestionModel> {
        return try {
            val gson = Gson()
            val listType = object : TypeToken<List<QuestionModel>>() {}.type
            gson.fromJson(jsonString, listType)
        } catch (e: Exception) {
            Log.e("JsonError", "Lỗi convert: ${e.message}")
            emptyList()
        }
    }

    // --- CÁC HÀM THỦ CÔNG & KHÁC ---

    private fun addManualQuestionToBuffer(): Boolean {
        try {
            val qText = etQuestion.text.toString().trim()
            val opA = etA.text.toString().trim()
            val opB = etB.text.toString().trim()
            val opC = etC.text.toString().trim()
            val opD = etD.text.toString().trim()

            if (qText.isEmpty() && opA.isEmpty() && opB.isEmpty() && opC.isEmpty() && opD.isEmpty()) {
                return true
            }

            if (qText.isEmpty() || opA.isEmpty() || opB.isEmpty() || opC.isEmpty() || opD.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return false
            }

            val selectedId = rgCorrect.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Hãy chọn đáp án đúng!", Toast.LENGTH_SHORT).show()
                return false
            }

            val correctContent = when (selectedId) {
                R.id.rbA -> opA
                R.id.rbB -> opB
                R.id.rbC -> opC
                R.id.rbD -> opD
                else -> opA
            }

            val newQuestion = QuestionModel(
                question = qText,
                options = listOf(opA, opB, opC, opD),
                correct = correctContent
            )
            questionsList.add(newQuestion)
            return true

        } catch (e: Exception) {
            return false
        }
    }

    private fun resetManualInputs() {
        etQuestion.setText("")
        etA.setText("")
        etB.setText("")
        etC.setText("")
        etD.setText("")
        rgCorrect.clearCheck()
        etQuestion.requestFocus()
    }

    private fun finishQuiz() {
        val title = etTitle.text.toString().trim()
        val time = etTime.text.toString().trim()

        if (title.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Chưa nhập Tên hoặc Thời gian!", Toast.LENGTH_SHORT).show()
            return
        }

        if (etQuestion.text.toString().isNotEmpty()) {
            val isSuccess = addManualQuestionToBuffer()
            if (!isSuccess) return
        }

        if (questionsList.isEmpty()) {
            Toast.makeText(this, "Chưa có câu hỏi nào!", Toast.LENGTH_SHORT).show()
            return
        }

        saveQuizToFirebase(title, time)
    }

    private fun saveQuizToFirebase(title: String, time: String) {
        try {
            val ref = FirebaseDatabase.getInstance(databaseUrl).getReference("Quizzes")

            val quizId = System.currentTimeMillis().toString()

            val newQuiz = QuizModel(
                id = quizId,
                title = title,
                subtitle = "Tổng hợp (${questionsList.size} câu)",
                time = time,
                questionList = questionsList
            )

            ref.child(quizId).setValue(newQuiz)
                .addOnSuccessListener {
                    Toast.makeText(this, "Lưu thành công!", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi Server: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi App: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateQuestionCountUI() {
        tvCount.text = "Tổng câu hỏi: ${questionsList.size}"
    }
}