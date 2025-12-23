package com.tien.quizapp

// Đây là file chứa cấu trúc dữ liệu người dùng
// Phải nằm riêng lẻ, không được nằm trong class khác
data class UserModel(
    val fullName: String = "",
    val score: Int = 0,
    val gender: String = "",
    val dob: String = "",

    // Các trường phục vụ Nhiệm vụ hàng ngày (Daily Quest)
    val dailyQuestProgress: Int = 0,
    val lastTaskDate: String = "",
    val isRewardClaimed: Boolean = false
)