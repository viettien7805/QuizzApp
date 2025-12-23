package com.tien.quizapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(
    private val userList: List<UserModel>,
    private val startRank: Int
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    // Lớp ViewHolder giúp ánh xạ các View từ XML vào Kotlin
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // Ánh xạ đúng ID từ file item_player_rank.xml
        val tvRank: TextView = v.findViewById(R.id.rank_number)
        val imgAvatar: ImageView = v.findViewById(R.id.avatar)
        val tvName: TextView = v.findViewById(R.id.player_name)
        val tvUsername: TextView = v.findViewById(R.id.player_username)
        val tvScore: TextView = v.findViewById(R.id.player_score)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_rank, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]

        // 1. Hiển thị thứ hạng (cộng dồn từ 4 trở đi)
        holder.tvRank.text = (position + startRank).toString()

        // 2. Hiển thị tên đầy đủ từ Firebase
        holder.tvName.text = user.fullName

        // 3. Hiển thị Username giả định hoặc dùng tên không dấu
        holder.tvUsername.text = "@${user.fullName.replace(" ", "_").lowercase()}"

        // 4. Hiển thị điểm số
        holder.tvScore.text = user.score.toString()

        // 5. Đổ ảnh Avatar dựa trên giới tính Nam/Nữ từ Firebase
        if (user.gender == "Nữ") {
            holder.imgAvatar.setImageResource(R.drawable.avatar_female)
        } else {
            holder.imgAvatar.setImageResource(R.drawable.avatar_male)
        }
    }

    override fun getItemCount() = userList.size
}