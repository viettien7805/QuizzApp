package com.tien.quizapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvName: TextView
    private lateinit var imgAvatar: ImageView

    // 1. Tạo bộ lắng nghe kết quả từ ProfileActivity
    private val profileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Lấy dữ liệu mới vừa sửa xong
            val newName = result.data?.getStringExtra("NEW_NAME")
            val newGender = result.data?.getStringExtra("NEW_GENDER")

            // CẬP NHẬT GIAO DIỆN NGAY LẬP TỨC (Không cần chờ tải lại)
            if (newName != null) tvName.text = newName

            if (newGender == "Nữ") {
                imgAvatar.setImageResource(R.drawable.avatar_female)
            } else {
                imgAvatar.setImageResource(R.drawable.avatar_male)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()

        // Ánh xạ View
        val btnProfile = findViewById<LinearLayout>(R.id.card_go_profile)
        val btnLogout = findViewById<Button>(R.id.btn_final_logout)
        val btnBack = findViewById<ImageView>(R.id.btn_back_settings)
        val btnChangePass = findViewById<LinearLayout>(R.id.btn_change_pass)
        val btnSupport = findViewById<LinearLayout>(R.id.btn_support_mail)

        tvName = findViewById(R.id.tv_name_summary)
        imgAvatar = findViewById(R.id.img_avatar_summary)

        // Load dữ liệu lần đầu
        loadUserData()

        // 2. Khi bấm vào sửa hồ sơ -> Dùng launcher đã tạo ở trên
        btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            profileLauncher.launch(intent) // Dùng launch thay vì startActivity
        }

        // Các nút khác giữ nguyên
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnChangePass.setOnClickListener {
            auth.currentUser?.email?.let { email ->
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { Toast.makeText(this, "Đã gửi mail đổi mật khẩu!", Toast.LENGTH_SHORT).show() }
            }
        }

        btnSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:hotro@quizapp.com")
            }
            startActivity(intent)
        }

        btnBack.setOnClickListener { finish() }
    }

    // Hàm load dữ liệu bình thường (chạy khi mới vào màn hình)
    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            tvName.text = user.displayName ?: "Chưa đặt tên"

            // Lấy giới tính từ Firebase để hiện avatar đúng
            val db = FirebaseDatabase.getInstance().getReference("users")
            db.child(user.uid).child("gender").get().addOnSuccessListener {
                if (it.exists() && it.value == "Nữ") {
                    imgAvatar.setImageResource(R.drawable.avatar_female)
                } else {
                    imgAvatar.setImageResource(R.drawable.avatar_male)
                }
            }
        }
    }
}