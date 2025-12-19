package com.tien.quizapp

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tien.quizapp.databinding.ActivityLeaderboardBinding

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Khởi tạo binding đầu tiên để tránh văng app
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Xử lý nút Back
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 3. Xử lý chuyển đổi Tab (Weekly / All Time)
        setupTabSelection()
    }

    private fun setupTabSelection() {
        binding.apply {
            // Mặc định ban đầu chọn Weekly
            tabWeekly.setOnClickListener {
                updateTabUI(isWeeklySelected = true)
                // TODO: Gọi hàm load dữ liệu Weekly từ Firebase tại đây
            }

            tabAlltime.setOnClickListener {
                updateTabUI(isWeeklySelected = false)
                // TODO: Gọi hàm load dữ liệu All Time từ Firebase tại đây
            }
        }
    }

    private fun updateTabUI(isWeeklySelected: Boolean) {
        binding.apply {
            if (isWeeklySelected) {
                // Weekly Active: Nền tím, All Time: Trong suốt
                tabWeekly.setBackgroundResource(R.drawable.tab_weekly_active)
                tabAlltime.setBackgroundResource(android.R.color.transparent)
            } else {
                // All Time Active: Nền tím, Weekly: Trong suốt
                tabAlltime.setBackgroundResource(R.drawable.tab_weekly_active)
                tabWeekly.setBackgroundResource(android.R.color.transparent)
            }

            // Luôn giữ chữ màu trắng cho cả 2 nút
            tabWeekly.setTextColor(Color.WHITE)
            tabAlltime.setTextColor(Color.WHITE)
        }
    }
}