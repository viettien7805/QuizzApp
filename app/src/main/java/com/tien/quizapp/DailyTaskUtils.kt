package com.tien.quizapp

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DailyTaskUtils {

    // Hàm này gọi khi hoàn thành 1 bài thi (Gọi ở QuizActivity)
    fun incrementProgress(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val uid = currentUser.uid
        val ref = FirebaseDatabase.getInstance().getReference("users").child(uid)

        // Đọc dữ liệu cũ về để kiểm tra ngày tháng
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val today = getTodayString()

                // Lấy ngày chơi lần cuối trên Firebase
                val lastDate = snapshot.child("lastTaskDate").getValue(String::class.java) ?: ""

                // Lấy tiến độ hiện tại
                var currentProgress = snapshot.child("dailyQuestProgress").getValue(Int::class.java) ?: 0

                if (lastDate != today) {
                    // Nếu ngày lưu trên mạng khác ngày hôm nay -> Reset về 1 (vì vừa chơi xong 1 ván)
                    currentProgress = 1
                } else {
                    // Nếu trùng ngày -> Cộng tiếp
                    currentProgress++
                }

                // Cập nhật lại cả Ngày và Tiến độ lên Firebase
                val updates = mapOf<String, Any>(
                    "lastTaskDate" to today,
                    "dailyQuestProgress" to currentProgress
                )

                ref.updateChildren(updates).addOnFailureListener {
                    Toast.makeText(context, "Lỗi cập nhật nhiệm vụ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) { }
        })
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // LƯU Ý: Các hàm getProgress, isRewardClaimed cũ phải XÓA BỎ.
    // Vì với Firebase, chúng ta không dùng hàm trả về kết quả ngay (return Int) được,
    // mà phải dùng Listener ở MainActivity như mình đã hướng dẫn ở câu trước.
}