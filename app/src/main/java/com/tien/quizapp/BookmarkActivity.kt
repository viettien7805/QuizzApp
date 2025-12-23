package com.tien.quizapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.tien.quizapp.databinding.ActivityBookmarkBinding

class BookmarkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarkBinding
    private lateinit var bookmarkList: MutableList<QuizModel>
    private lateinit var adapter: QuizListAdapter // Tái sử dụng Adapter của màn hình chính

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookmarkList = mutableListOf()

        // Cài đặt RecyclerView dạng lưới 2 cột
        adapter = QuizListAdapter(bookmarkList) { quiz ->
            // Khi bấm vào bài thi -> Chuyển sang chơi
            // (Copy logic từ MainActivity)
            MainActivity.questionListGlobal = quiz.questionList
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("id", quiz.id)
            intent.putExtra("time", quiz.time.toString())
            startActivity(intent)
        }

        binding.rvBookmarks.layoutManager = GridLayoutManager(this, 2)
        binding.rvBookmarks.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        loadBookmarks()
    }

    private fun loadBookmarks() {
        binding.progressBar.visibility = View.VISIBLE
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            binding.progressBar.visibility = View.GONE
            return
        }

        // 1. Lấy danh sách ID đã bookmark
        val bookmarksRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("bookmarks")

        bookmarksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookmarkedIds = mutableListOf<String>()
                for (snap in snapshot.children) {
                    // Key chính là QuizID (ví dụ: -Ne8s...)
                    bookmarkedIds.add(snap.key ?: "")
                }

                if (bookmarkedIds.isEmpty()) {
                    // Nếu không có bookmark nào
                    showEmptyState(true)
                } else {
                    // 2. Có ID rồi -> đi lấy chi tiết bài thi
                    fetchQuizzesDetails(bookmarkedIds)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun fetchQuizzesDetails(ids: List<String>) {
        val quizzesRef = FirebaseDatabase.getInstance().getReference("Quizzes")

        quizzesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bookmarkList.clear()
                for (snap in snapshot.children) {
                    val quizId = snap.key ?: ""

                    // Kiểm tra: Nếu bài thi này có ID nằm trong danh sách bookmark
                    if (ids.contains(quizId)) {
                        val quizModel = snap.getValue(QuizModel::class.java)
                        quizModel?.let {
                            it.id = quizId
                            bookmarkList.add(it)
                        }
                    }
                }

                // Cập nhật giao diện
                adapter.notifyDataSetChanged()
                showEmptyState(bookmarkList.isEmpty())
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.progressBar.visibility = View.GONE
        if (isEmpty) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvBookmarks.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvBookmarks.visibility = View.VISIBLE
        }
    }
}