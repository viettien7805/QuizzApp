package com.tien.quizapp
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messageList: ArrayList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAi: TextView = view.findViewById(R.id.tvAiMessage)
        val tvUser: TextView = view.findViewById(R.id.tvUserMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // Nhớ đảm bảo bạn đã có file item_chat_message.xml (xem lại bài trước nếu thiếu)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messageList[position]
        if (msg.isUser) {
            holder.tvUser.text = msg.message
            holder.tvUser.visibility = View.VISIBLE
            holder.tvAi.visibility = View.GONE
        } else {
            holder.tvAi.text = msg.message
            holder.tvAi.visibility = View.VISIBLE
            holder.tvUser.visibility = View.GONE
        }
    }
    override fun getItemCount() = messageList.size
}