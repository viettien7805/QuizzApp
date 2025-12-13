package com.tien.quizapp

import android.util.Log
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizAIHelper {

    private val apiKey = "AIzaSyAKEglKOpYDZeHNJGFBxXzghx_Qf9o0Zf0"

    // Khởi tạo model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = apiKey
    )

    private var chatHistory: Chat? = null

    suspend fun generateQuizFromTopic(topic: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Bạn là trợ lý tạo câu hỏi trắc nghiệm JSON.
                    Chủ đề: "$topic".
                    Yêu cầu: Tạo 5 câu hỏi tiếng Việt.
                    Output: Chỉ trả về JSON Array, không markdown.
                    Mẫu: [{"question":"...","options":["A","B","C","D"],"correct_answer":"A"}]
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                var cleanText = response.text?.trim() ?: ""
                cleanText = cleanText.replace("```json", "").replace("```", "").trim()
                return@withContext cleanText
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    // Hàm 2: Chat hỏi đáp
    suspend fun chatWithAI(message: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Nếu chưa có lịch sử (lần đầu chat), thì khởi tạo cuộc trò chuyện mới
                if (chatHistory == null) {
                    chatHistory = generativeModel.startChat()
                }

                // Gửi tin nhắn vào cuộc trò chuyện ĐANG CÓ (nó sẽ tự nhớ các câu trước)
                val response = chatHistory?.sendMessage(message)

                return@withContext response?.text?.trim()
            } catch (e: Exception) {
                e.printStackTrace()
                // Nếu lỗi quá nặng, có thể reset lịch sử để thử lại lần sau
                chatHistory = null
                return@withContext "Lỗi kết nối: ${e.message}"
            }
        }
    }

    fun clearChatHistory() {
        chatHistory = null
    }
}