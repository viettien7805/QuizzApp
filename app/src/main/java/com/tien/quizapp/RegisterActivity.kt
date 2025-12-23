package com.tien.quizapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        // Ánh xạ View
        val edtEmail = findViewById<EditText>(R.id.edt_email_reg)
        val edtPass = findViewById<EditText>(R.id.edt_password_reg)
        val edtRePass = findViewById<EditText>(R.id.edt_repassword_reg)
        val btnRegister = findViewById<Button>(R.id.btn_register_final)
        val tvGoLogin = findViewById<TextView>(R.id.tv_go_login)

        // Nút quay lại
        tvGoLogin.setOnClickListener { finish() }

        // Nút Đăng ký
        btnRegister.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val pass = edtPass.text.toString().trim()
            val rePass = edtRePass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty() || rePass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != rePass) {
                Toast.makeText(this, "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "Mật khẩu phải trên 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createAccount(email, pass)
        }
    }

    private fun createAccount(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                // Gửi email xác thực
                val user = result.user
                user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        Toast.makeText(this, "Đăng ký thành công! Hãy kiểm tra Email để xác thực.", Toast.LENGTH_LONG).show()
                        auth.signOut() // Đăng xuất ngay để bắt người dùng đăng nhập lại
                        finish() // Quay về màn hình Login
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(this, "Gửi email xác thực thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Đăng ký lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}