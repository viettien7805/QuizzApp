package com.tien.quizapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.tien.quizapp.databinding.ActivityLeaderboardBinding

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private val databaseUrl = "https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val userList = mutableListOf<UserModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        setupTabs()
        loadData()
    }

    private fun loadData() {
        val ref = FirebaseDatabase.getInstance(databaseUrl).getReference("users")

        // Sắp xếp theo điểm score
        ref.orderByChild("score").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (snap in snapshot.children) {
                    val user = snap.getValue(UserModel::class.java)
                    user?.let { userList.add(it) }
                }
                // Đảo ngược để điểm cao nhất lên đầu
                userList.reverse()

                if (userList.isNotEmpty()) {
                    updatePodiumUI()
                    updateListUI()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LeaderboardActivity, "Lỗi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePodiumUI() {
        binding.apply {
            // Hạng 1
            if (userList.size >= 1) {
                val u = userList[0]
                tvName1st.text = u.fullName //
                tvScore1st.text = u.score.toString() // Gán điểm số
                avatar1.setImageResource(getAvatar(u.gender))
            }
            // Hạng 2
            if (userList.size >= 2) {
                val u = userList[1]
                tvName2nd.text = u.fullName
                tvScore2nd.text = u.score.toString() // Gán điểm số
                avatar2.setImageResource(getAvatar(u.gender))
            }
            // Hạng 3
            if (userList.size >= 3) {
                val u = userList[2]
                tvName3rd.text = u.fullName
                tvScore3rd.text = u.score.toString() // Gán điểm số
                avatar3.setImageResource(getAvatar(u.gender))
            }
        }
    }

    private fun updateListUI() {
        // Lấy danh sách từ người thứ 4 trở đi
        val list = if (userList.size > 3) userList.subList(3, userList.size) else listOf()

        // Truyền startRank = 4
        val adapter = LeaderboardAdapter(list, 4)
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.rvLeaderboard.adapter = adapter
    }

    private fun getAvatar(gender: String): Int {
        return if (gender == "Nữ") R.drawable.avatar_female else R.drawable.avatar_male
    }

    private fun setupTabs() {
        binding.tabWeekly.setOnClickListener {
            binding.tabWeekly.setBackgroundResource(R.drawable.tab_weekly_active)
            binding.tabAlltime.setBackgroundResource(android.R.color.transparent)
        }
        binding.tabAlltime.setOnClickListener {
            binding.tabAlltime.setBackgroundResource(R.drawable.tab_weekly_active)
            binding.tabWeekly.setBackgroundResource(android.R.color.transparent)
        }
    }
}