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

    // Hàm 2: Tạo Quiz (Đã sửa Prompt để cấm LaTeX)
    suspend fun generateQuizFromTopic(topic: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Prompt nâng cấp: Tự động bắt số lượng câu hỏi và cấm LaTeX
                val prompt = """
                    Bạn là trợ lý tạo câu hỏi trắc nghiệm JSON.
                    Nội dung yêu cầu từ người dùng: "$topic".
                    
                    NHIỆM VỤ:
                    1. Phân tích yêu cầu trên để xác định chủ đề và SỐ LƯỢNG câu hỏi.
                    2. Nếu người dùng CÓ nói số lượng (VD: "10 câu", "20 câu"), hãy tạo ĐÚNG số lượng đó.
                    3. Nếu người dùng KHÔNG nói số lượng, mặc định tạo 5 câu.
                    
                    QUY ĐỊNH ĐỊNH DẠNG (BẮT BUỘC):
                    - KHÔNG dùng định dạng LaTeX (dấu ${'$'}). Hãy dùng văn bản thường (VD: dùng "căn bậc 2", "x^2").
                    - Output chỉ trả về duy nhất một JSON Array.
                    
                    Mẫu JSON: [{"question":"...","options":["A","B","C","D"],"correct_answer":"A"}]
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