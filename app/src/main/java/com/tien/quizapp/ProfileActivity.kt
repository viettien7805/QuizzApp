package com.tien.quizapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val databaseRef = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        // Ánh xạ View
        val imgAvatar = findViewById<ImageView>(R.id.img_avatar_detail)
        val edtName = findViewById<EditText>(R.id.edt_name_detail)
        val edtDob = findViewById<EditText>(R.id.edt_dob_detail)
        val rgGender = findViewById<RadioGroup>(R.id.rg_gender_detail)
        val rbMale = findViewById<RadioButton>(R.id.rb_male_detail)
        val rbFemale = findViewById<RadioButton>(R.id.rb_female_detail)
        val btnSave = findViewById<Button>(R.id.btn_save_profile)
        val btnBack = findViewById<ImageView>(R.id.btn_back_profile)

        // 1. Load dữ liệu hiện tại
        user?.let { firebaseUser ->
            edtName.setText(firebaseUser.displayName)

            // Lấy thêm thông tin từ Database
            databaseRef.child(firebaseUser.uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val dob = snapshot.child("dob").value.toString()
                    val gender = snapshot.child("gender").value.toString()

                    edtDob.setText(dob)
                    if (gender == "Nữ") {
                        rbFemale.isChecked = true
                        imgAvatar.setImageResource(R.drawable.avatar_female)
                    } else {
                        rbMale.isChecked = true
                        imgAvatar.setImageResource(R.drawable.avatar_male)
                    }
                }
            }
        }

        // 2. Logic đổi Avatar khi chọn giới tính
        rgGender.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_male_detail) {
                imgAvatar.setImageResource(R.drawable.avatar_male)
            } else {
                imgAvatar.setImageResource(R.drawable.avatar_female)
            }
        }

        // 3. Chọn ngày sinh
        edtDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this, { _, y, m, d ->
                edtDob.setText("$d/${m + 1}/$y")
            }, year, month, day)
            dpd.show()
        }

        // 4. Lưu thông tin (Đoạn quan trọng đã sửa)
        btnSave.setOnClickListener {
            val name = edtName.text.toString().trim()
            val dob = edtDob.text.toString().trim()
            val gender = if (rbMale.isChecked) "Nam" else "Nữ"

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cập nhật Auth (Tên hiển thị)
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user?.updateProfile(profileUpdates)

            // Cập nhật Database
            val userData = mapOf("fullName" to name, "dob" to dob, "gender" to gender)
            user?.uid?.let { uid ->
                databaseRef.child(uid).setValue(userData).addOnSuccessListener {
                    Toast.makeText(this, "Đã lưu hồ sơ!", Toast.LENGTH_SHORT).show()

                    // --- SỬA Ở ĐÂY: Trả dữ liệu về SettingsActivity ---
                    val returnIntent = Intent()
                    returnIntent.putExtra("NEW_NAME", name)
                    returnIntent.putExtra("NEW_GENDER", gender)

                    // Gửi tín hiệu OK kèm dữ liệu
                    setResult(Activity.RESULT_OK, returnIntent)
                    // -------------------------------------------------

                    finish() // Đóng màn hình
                }
            }
        }

        btnBack.setOnClickListener { finish() }
    }
}