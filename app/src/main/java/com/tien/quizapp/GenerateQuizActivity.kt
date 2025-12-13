package com.tien.quizapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class GenerateQuizActivity : AppCompatActivity() {

    // Khai báo biến cho Chat
    private val messageList = ArrayList<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var rvChat: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_quiz)

        val etTopic = findViewById<EditText>(R.id.etTopic)
        val btnGenerate = findViewById<Button>(R.id.btnGenerate)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val swChatMode = findViewById<SwitchMaterial>(R.id.swChatMode)
        rvChat = findViewById(R.id.chatRecyclerView)

        // Setup RecyclerView (Danh sách chat)
        chatAdapter = ChatAdapter(messageList)
        rvChat.adapter = chatAdapter
        rvChat.layoutManager = LinearLayoutManager(this)

        val quizAIHelper = QuizAIHelper()

        // Lời chào đầu tiên
        addMessageToChat("Chào bạn! Bạn muốn tạo Quiz hay trò chuyện? Hãy gạt công tắc ở trên nhé!", false)

        // Xử lý khi gạt công tắc
        swChatMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                btnGenerate.text = "Gửi"
                etTopic.hint = "Hỏi AI bất cứ điều gì..."
                addMessageToChat("Đã chuyển sang chế độ Chat. Bạn hỏi đi!", false)
            } else {
                btnGenerate.text = "Tạo Quiz"
                etTopic.hint = "Nhập chủ đề (VD: Lịch sử)..."
                addMessageToChat("Đã chuyển sang chế độ Tạo Quiz.", false)
            }
        }

        btnGenerate.setOnClickListener {
            val input = etTopic.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Chưa nhập nội dung!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ẩn bàn phím, hiện loading
            progressBar.visibility = View.VISIBLE
            btnGenerate.isEnabled = false
            etTopic.setText("") // Xóa ô nhập sau khi bấm

            CoroutineScope(Dispatchers.Main).launch {
                if (swChatMode.isChecked) {
                    // --- LOGIC CHAT LIÊN TỤC ---
                    // 1. Hiện câu hỏi của bạn lên màn hình ngay
                    addMessageToChat(input, true)

                    // 2. Gọi AI
                    val answer = quizAIHelper.chatWithAI(input)

                    // 3. Hiện câu trả lời
                    addMessageToChat(answer ?: "Lỗi kết nối mạng!", false)

                } else {
                    // --- LOGIC TẠO QUIZ ---
                    addMessageToChat("Đang tạo bộ câu hỏi về: $input ...", true)

                    val jsonResult = quizAIHelper.generateQuizFromTopic(input)

                    if (jsonResult != null) {
                        try {
                            val questions = parseJsonToQuestions(jsonResult)

                            // Gán dữ liệu và chuyển màn hình
                            QuizActivity.questionModelList = questions
                            QuizActivity.time = "5"

                            val intent = Intent(this@GenerateQuizActivity, QuizActivity::class.java)
                            startActivity(intent)

                            addMessageToChat("✅ Đã tạo xong! Chúc may mắn.", false)
                        } catch (e: Exception) {
                            addMessageToChat("❌ Lỗi dữ liệu AI: ${e.message}", false)
                        }
                    } else {
                        addMessageToChat("❌ AI không phản hồi.", false)
                    }
                }

                progressBar.visibility = View.GONE
                btnGenerate.isEnabled = true
            }
        }
    }

    // Hàm phụ: Thêm tin nhắn vào danh sách và cuộn xuống dưới cùng
    private fun addMessageToChat(msg: String, isUser: Boolean) {
        messageList.add(ChatMessage(msg, isUser))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        rvChat.smoothScrollToPosition(messageList.size - 1)
    }

    // Hàm phụ: Phân tích JSON (Giữ nguyên như cũ)
    private fun parseJsonToQuestions(jsonString: String): List<QuestionModel> {
        val list = mutableListOf<QuestionModel>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val q = QuestionModel(
                obj.getString("question"),
                mutableListOf<String>().apply {
                    val arr = obj.getJSONArray("options")
                    for (j in 0 until arr.length()) add(arr.getString(j))
                },
                obj.getString("correct_answer")
            )
            list.add(q)
        }
        return list
    }
}