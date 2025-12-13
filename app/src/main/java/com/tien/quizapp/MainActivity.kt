package com.tien.quizapp;

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.tien.quizapp.databinding.ActivityMainBinding
import com.tien.quizapp.R

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var quizModelList : MutableList<QuizModel>
    lateinit var adapter: QuizListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quizModelList = mutableListOf()
        getDataFromFirebase()


    }

    private fun setupRecyclerView(){
        binding.progressBar.visibility = View.GONE
        adapter = QuizListAdapter(quizModelList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun getDataFromFirebase(){
        binding.progressBar.visibility = View.VISIBLE

        val databaseUrl = "https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/"
        FirebaseDatabase.getInstance("https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
            .get()
            .addOnSuccessListener { dataSnapshot->
                if(dataSnapshot.exists()){
                    for (snapshot in dataSnapshot.children){
                        val quizModel = snapshot.getValue(QuizModel::class.java)
                        if (quizModel != null) {
                            quizModelList.add(quizModel)
                        }
                    }
                }
                setupRecyclerView()
            }


    }
}














