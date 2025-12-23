package com.tien.quizapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.tien.quizapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var quizModelList: MutableList<QuizModel>
    private lateinit var adapter: QuizListAdapter
    private lateinit var auth: FirebaseAuth

    // URL Database
    private val databaseUrl = "https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/"

    companion object {
        var questionListGlobal: List<QuestionModel> = listOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        quizModelList = mutableListOf()

        setupRecyclerView()
        setupBottomNavigation()
        setupActionButtons()

        loadUserInfo()
        getDataFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        // Chỉ gọi hàm này để cập nhật nhiệm vụ và info
        loadUserInfo()
        loadDailyQuest()
    }

    // --- LOGIC 1: CÁC NÚT CHỨC NĂNG ---
    private fun setupActionButtons() {
        binding.btnAiQuiz.setOnClickListener {
            startActivity(Intent(this, GenerateQuizActivity::class.java))
        }
        binding.btnCreateManual.setOnClickListener {
            startActivity(Intent(this, CreateQuizActivity::class.java))
        }
    }

    // --- LOGIC 2: THÔNG TIN NGƯỜI DÙNG ---
    private fun loadUserInfo() {
        val user = auth.currentUser ?: return
        binding.tvUserNameMain.text = "Xin chào, ${user.displayName ?: "Bạn"}"

        val userRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users").child(user.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Lấy điểm tổng
                    val score = snapshot.child("score").getValue(Int::class.java) ?: 0
                    binding.tvTotalScore.text = score.toString()

                    // Lấy Avatar
                    val gender = snapshot.child("gender").value.toString()
                    binding.imgAvatarMain.setImageResource(
                        if (gender == "Nữ") R.drawable.avatar_female else R.drawable.avatar_male
                    )
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- LOGIC 3: NHIỆM VỤ HÀNG NGÀY (ĐÃ SỬA) ---
    private fun loadDailyQuest() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // Chưa đăng nhập -> Reset về 0
            binding.pbDailyTask.progress = 0
            binding.tvDailyProgress.text = "0/3"
            return
        }

        val userId = currentUser.uid
        val ref = FirebaseDatabase.getInstance().getReference("users").child(userId)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 1. Lấy dữ liệu từ Firebase
                var progress = snapshot.child("dailyQuestProgress").getValue(Int::class.java) ?: 0
                val lastDate = snapshot.child("lastTaskDate").getValue(String::class.java) ?: ""
                val isRewardClaimed = snapshot.child("isRewardClaimed").getValue(Boolean::class.java) ?: false

                // 2. Kiểm tra ngày (Reset nếu qua ngày mới)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (today != lastDate) {
                    progress = 0
                }

                val target = 3

                // 3. Cập nhật UI (Sửa đúng ID: pbDailyTask và tvDailyProgress)
                binding.pbDailyTask.max = target
                binding.pbDailyTask.progress = progress
                binding.tvDailyProgress.text = "$progress/$target"

                // 4. Kiểm tra nhận thưởng (Tự động nhận hoặc hiện nút)
                // Logic cũ của bạn là tự động nhận, mình giữ nguyên logic đó nhưng thêm check an toàn
                if (progress >= target && !isRewardClaimed) {
                    giveDailyReward(userId)
                }
            }

            override fun onCancelled(error: DatabaseError) { }
        })
    }

    private fun giveDailyReward(userId: String) {
        val userRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users").child(userId)

        // Dùng runTransaction để cộng điểm và đánh dấu đã nhận cùng lúc
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                // Kiểm tra lại lần nữa trong transaction để tránh cộng nhiều lần
                val isClaimed = mutableData.child("isRewardClaimed").getValue(Boolean::class.java) ?: false
                if (isClaimed) {
                    return Transaction.success(mutableData) // Đã nhận rồi thì thôi
                }

                // Cộng điểm
                val currentScore = mutableData.child("score").getValue(Int::class.java) ?: 0
                mutableData.child("score").value = currentScore + 50

                // Đánh dấu đã nhận
                mutableData.child("isRewardClaimed").value = true

                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Nhiệm vụ hoàn thành! +50 Xu", Toast.LENGTH_LONG).show()
                        // Cập nhật điểm hiển thị ngay lập tức
                        val newScore = snapshot?.child("score")?.getValue(Int::class.java) ?: 0
                        binding.tvTotalScore.text = newScore.toString()
                    }
                }
            }
        })
    }

    // --- LOGIC 4: DANH SÁCH BÀI THI ---
    private fun setupRecyclerView() {
        adapter = QuizListAdapter(quizModelList) { quiz ->
            questionListGlobal = quiz.questionList
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("id", quiz.id)
            intent.putExtra("time", quiz.time.toString())
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
    }

    private fun getDataFromFirebase() {
        binding.progressBar.visibility = View.VISIBLE // Lưu ý: Đây là progressBar loading (giữa màn hình), ko phải thanh nhiệm vụ
        FirebaseDatabase.getInstance(databaseUrl).getReference("Quizzes")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    quizModelList.clear()
                    for (item in snapshot.children) {
                        val quizModel = item.getValue(QuizModel::class.java)
                        quizModel?.let {
                            it.id = item.key ?: ""
                            quizModelList.add(0, it)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                }
                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_leaderboard -> {
                    startActivity(Intent(this, LeaderboardActivity::class.java))
                    true
                }
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}