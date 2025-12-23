package com.tien.quizapp

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // 1. Cấu hình Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        // 2. Cấu hình Facebook
        callbackManager = CallbackManager.Factory.create()

        // Ánh xạ View
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val edtEmail = findViewById<EditText>(R.id.edt_email)
        val edtPass = findViewById<EditText>(R.id.edt_password)
        val btnGoogle = findViewById<ImageButton>(R.id.btn_google)
        val btnFacebook = findViewById<ImageButton>(R.id.btn_facebook)

        // --- THÊM ÁNH XẠ NÚT QUÊN MẬT KHẨU ---
        // (Bạn nhớ kiểm tra trong activity_login.xml đã đặt ID này chưa nhé)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // --- XỬ LÝ SỰ KIỆN ---

        // A. Đăng nhập Email thường
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val pass = edtPass.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        // Kiểm tra xác thực email
                        if (user != null && user.isEmailVerified) {
                            goMain()
                        } else {
                            Toast.makeText(this, "Vui lòng vào Email xác thực tài khoản trước!", Toast.LENGTH_LONG).show()
                            auth.signOut()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Nhập thiếu thông tin!", Toast.LENGTH_SHORT).show()
            }
        }

        // B. Chuyển sang đăng ký
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // C. Nút Google
        btnGoogle.setOnClickListener {
            val intent = googleClient.signInIntent
            startActivityForResult(intent, 1001)
        }

        // D. Nút Facebook
        btnFacebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
            setupFacebookCallback()
        }

        // E. Nút Quên mật khẩu (MỚI THÊM)
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    // --- HÀM HIỂN THỊ DIALOG QUÊN MẬT KHẨU ---
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        builder.setView(view)

        val dialog = builder.create()
        // Làm nền dialog trong suốt để bo góc đẹp
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        // Ánh xạ view trong dialog
        val etEmailReset = view.findViewById<EditText>(R.id.etEmailReset)
        val btnSend = view.findViewById<Button>(R.id.btnSend)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSend.setOnClickListener {
            val email = etEmailReset.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Email!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendResetEmail(email, dialog)
        }
    }

    // --- HÀM GỬI EMAIL RESET (FIREBASE) ---
    private fun sendResetEmail(email: String, dialog: AlertDialog) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã gửi email khôi phục! Hãy kiểm tra hộp thư.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Hàm callback riêng cho Facebook
    private fun setupFacebookCallback() {
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val token = result.accessToken
                val credential = FacebookAuthProvider.getCredential(token.token)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { goMain() }
                    .addOnFailureListener {
                        Toast.makeText(this@LoginActivity, "Lỗi FB Firebase: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            override fun onCancel() { Toast.makeText(this@LoginActivity, "Đã hủy login FB", Toast.LENGTH_SHORT).show() }
            override fun onError(error: FacebookException) {
                Toast.makeText(this@LoginActivity, "Lỗi FB SDK: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Nhận kết quả trả về
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 1. Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // 2. Google
        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { goMain() }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi Google Firebase: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google thất bại: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        if (user != null) {
            val isSocialLogin = user.providerData.any { it.providerId == "facebook.com" || it.providerId == "google.com" }

            if (isSocialLogin || user.isEmailVerified) {
                goMain()
            }
        }
    }

    private fun goMain() {
        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}