package com.tien.quizapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.FirebaseDatabase // <--- Import Firebase
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

        // 1. Ánh xạ các View
        val etTopic = findViewById<EditText>(R.id.etTopic)
        val btnGenerate = findViewById<Button>(R.id.btnGenerate)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val swChatMode = findViewById<SwitchMaterial>(R.id.swChatMode)
        rvChat = findViewById(R.id.chatRecyclerView)

        // 2. TÌM NÚT THOÁT VÀ BẮT SỰ KIỆN
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Đóng màn hình này, quay về MainActivity
        }

        // Setup RecyclerView (Danh sách chat)
        chatAdapter = ChatAdapter(messageList)
        rvChat.adapter = chatAdapter

        // Cấu hình để list tự cuộn xuống dưới cùng
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvChat.layoutManager = layoutManager

        val quizAIHelper = QuizAIHelper()

        // Lời chào đầu tiên
        addMessageToChat("Chào bạn! Bạn muốn tạo Quiz hay trò chuyện? Hãy gạt công tắc ở trên nhé!", false)

        // Xử lý khi gạt công tắc
        swChatMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                btnGenerate.text = "Gửi"
                etTopic.hint = "Hỏi AI bất cứ điều gì..."
                addMessageToChat("💬 Đã chuyển sang chế độ Chat. Bạn hỏi đi!", false)
            } else {
                btnGenerate.text = "Tạo Quiz"
                etTopic.hint = "Nhập chủ đề (VD: Lịch sử)..."
                addMessageToChat("🎮 Đã chuyển sang chế độ Tạo Quiz.", false)
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
                    addMessageToChat(input, true)
                    val answer = quizAIHelper.chatWithAI(input)
                    addMessageToChat(answer ?: "Lỗi kết nối mạng!", false)

                } else {
                    // --- LOGIC TẠO QUIZ & LƯU FIREBASE ---
                    addMessageToChat("Đang tạo bộ câu hỏi về: $input ...", true)

                    val jsonResult = quizAIHelper.generateQuizFromTopic(input)

                    if (jsonResult != null) {
                        try {
                            val questions = parseJsonToQuestions(jsonResult)

                            if (questions.isNotEmpty()) {
                                // 1. LƯU VÀO FIREBASE REALTIME DATABASE
                                val databaseUrl = "https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/"
                                val ref = FirebaseDatabase.getInstance(databaseUrl).getReference("Quizzes")

                                // Tạo ID ngẫu nhiên dựa trên thời gian
                                val quizId = System.currentTimeMillis().toString()

                                // Tạo đối tượng QuizModel mới
                                // Lưu ý: Thời gian làm bài (time) sẽ bằng số lượng câu hỏi (ví dụ 10 câu = 10 phút)
                                val newQuiz = QuizModel(
                                    id = quizId,
                                    title = input, // Lấy nội dung nhập làm tiêu đề
                                    subtitle = "AI tạo (${questions.size} câu)",
                                    time = "${questions.size}",
                                    questionList = questions
                                )

                                // Đẩy lên Firebase
                                ref.child(quizId).setValue(newQuiz)

                                addMessageToChat("✅ Đã lưu bộ đề vào màn hình chính!", false)

                                // 2. CHUYỂN SANG MÀN HÌNH CHƠI NGAY


                                val intent = Intent(this@GenerateQuizActivity, QuizActivity::class.java)
                                intent.putExtra("id", quizId)       // Truyền ID vừa tạo
                                intent.putExtra("time", newQuiz.time) // Truyền thời gian
                                startActivity(intent)
                            } else {
                                addMessageToChat("⚠️ AI trả về dữ liệu rỗng. Thử lại nhé!", false)
                            }

                        } catch (e: Exception) {
                            addMessageToChat("❌ Lỗi xử lý dữ liệu: ${e.message}", false)
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

    // Hàm phụ: Phân tích JSON
    private fun parseJsonToQuestions(jsonString: String): List<QuestionModel> {
        val list = mutableListOf<QuestionModel>()
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}