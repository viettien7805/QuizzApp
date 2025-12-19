package com.tien.quizapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tien.quizapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var quizModelList : MutableList<QuizModel>
    private lateinit var adapter: QuizListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quizModelList = mutableListOf()

        // 1. Cấu hình RecyclerView (Danh sách game)
        setupRecyclerView()

        // 2. Lấy dữ liệu từ Firebase
        getDataFromFirebase()

        // 3. Cập nhật giao diện Daily Task (Nhiệm vụ hàng ngày)
        updateDailyTaskUI()

        // --- XỬ LÝ CÁC NÚT BẤM CHUYỂN MÀN HÌNH ---

        // Nút 1: Tạo Quiz bằng AI (Nút tím) -> Sang màn hình GenerateQuizActivity
        binding.btnAiQuiz.setOnClickListener {
            val intent = Intent(this, GenerateQuizActivity::class.java)
            startActivity(intent)
        }

        // Nút 2: Tạo thủ công (Nút cam) -> Sang màn hình CreateQuizActivity
        binding.btnCreateManual.setOnClickListener {
            val intent = Intent(this, CreateQuizActivity::class.java)
            startActivity(intent)
        }

        // --- XỬ LÝ MENU DƯỚI CÙNG (BOTTOM NAVIGATION) ---
        // Set mặc định chọn Home
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> {
                    // Đang ở Home thì không làm gì
                    true
                }

                R.id.nav_leaderboard -> {
                    // Chuyển sang màn hình Bảng xếp hạng
                    startActivity(Intent(this, LeaderboardActivity::class.java))
                    true
                }

                R.id.nav_profile -> {
                    // CẬP NHẬT: Chuyển sang màn hình Cài đặt (Settings)
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    // Hàm này chạy mỗi khi màn hình hiện lên (VD: Quay lại từ bài thi)
    // Giúp cập nhật ngay lập tức thanh tiến độ Daily Task
    override fun onResume() {
        super.onResume()
        updateDailyTaskUI()
        // Reset lại menu về Home để icon sáng đúng chỗ
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun updateDailyTaskUI() {
        // Mục tiêu: 3 bài mỗi ngày (Bạn có thể sửa số này)
        val target = 3

        // Lấy số bài đã làm từ PrefHelper (File tiện ích mình đã hướng dẫn tạo)
        val current = PrefHelper.getDailyProgress(this)

        // Cập nhật text: VD "1/3"
        binding.tvDailyScore.text = "$current/$target"

        // Tính phần trăm để chạy thanh progress
        val progressPercent = if (current >= target) 100 else (current * 100 / target)
        binding.progressBarDaily.progress = progressPercent

        // (Tùy chọn) Đổi màu chữ nếu hoàn thành
        if (current >= target) {
            binding.tvDailyScore.setTextColor(resources.getColor(R.color.purple_700))
        }
    }

    private fun setupRecyclerView(){
        // Grid 2 cột cho đẹp
        adapter = QuizListAdapter(quizModelList) { quizCanXoa ->
            showDeleteDialog(quizCanXoa)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun getDataFromFirebase(){
        val databaseUrl = "https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/"
        FirebaseDatabase.getInstance(databaseUrl).reference
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    quizModelList.clear()
                    if(snapshot.exists()){
                        for (item in snapshot.children){
                            val quizModel = item.getValue(QuizModel::class.java)
                            if (quizModel != null) {
                                quizModel.id = item.key ?: ""
                                // Thêm vào đầu danh sách để bài mới nhất lên trên
                                quizModelList.add(0, quizModel)
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun showDeleteDialog(quiz: QuizModel) {
        AlertDialog.Builder(this)
            .setTitle("Xóa bộ đề")
            .setMessage("Bạn có chắc muốn xóa '${quiz.title}' không?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteQuizFromFirebase(quiz)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteQuizFromFirebase(quiz: QuizModel) {
        val databaseUrl = "https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/"
        FirebaseDatabase.getInstance(databaseUrl).reference
            .child(quiz.id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi khi xóa: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}