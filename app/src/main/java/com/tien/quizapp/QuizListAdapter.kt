package com.tien.quizapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tien.quizapp.databinding.QuizItemRecyclerRowBinding

class QuizListAdapter(
    private val quizModelList: List<QuizModel>,
    private val onLongClick: (QuizModel) -> Unit // Callback xóa
) : RecyclerView.Adapter<QuizListAdapter.MyViewHolder>() {

    class MyViewHolder(val binding: QuizItemRecyclerRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: QuizModel, onLongClick: (QuizModel) -> Unit) {
            binding.apply {
                quizTitleText.text = model.title
                quizSubtitleText.text = model.subtitle
                quizTimeText.text = "${model.time} phút"

                // 1. Click thường: Chơi game (SỬA LỖI Ở ĐÂY)
                root.setOnClickListener {
                    val intent = Intent(root.context, QuizActivity::class.java)

                    // Thay vì gán biến tĩnh, ta gửi ID qua Intent
                    intent.putExtra("id", model.id)
                    intent.putExtra("time", model.time)

                    root.context.startActivity(intent)
                }

                // 2. Nhấn giữ: Gọi hàm xóa
                root.setOnLongClickListener {
                    onLongClick(model) // Báo ra ngoài MainActivity
                    true // return true để chặn click thường
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = QuizItemRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return quizModelList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(quizModelList[position], onLongClick)
    }
}