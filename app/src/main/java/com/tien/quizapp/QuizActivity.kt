package com.tien.quizapp

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.tien.quizapp.databinding.ActivityQuizBinding
import com.tien.quizapp.databinding.OptionItemBinding
import com.tien.quizapp.databinding.ScoreDialogBinding

class QuizActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityQuizBinding

    // Dữ liệu bộ đề
    private var questionModelList: MutableList<QuestionModel> = mutableListOf()
    private var currentQuestionIndex = 0
    private var selectedAnswer = ""
    private var score = 0

    // Biến trạng thái để ngăn người dùng chọn lại nhiều lần trong 1 câu
    private var isAnswerChecked = false

    // Timer
    private var timer: CountDownTimer? = null
    private var totalTimeInMillis = 0L
    private var remainingTimeInMillis = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val quizId = intent.getStringExtra("id") ?: ""
        val timeString = intent.getStringExtra("time") ?: "10"

        // Chuyển đổi thời gian từ phút sang mili giây
        totalTimeInMillis = timeString.toLong() * 60 * 1000L
        remainingTimeInMillis = totalTimeInMillis

        binding.apply {
            btnBack.setOnClickListener { finish() }
            btn5050.setOnClickListener(this@QuizActivity)
            btnSkip.setOnClickListener(this@QuizActivity)
            btnAddTime.setOnClickListener(this@QuizActivity)
            btnNext.setOnClickListener(this@QuizActivity)

            layoutOption1.root.setOnClickListener(this@QuizActivity)
            layoutOption2.root.setOnClickListener(this@QuizActivity)
            layoutOption3.root.setOnClickListener(this@QuizActivity)
            layoutOption4.root.setOnClickListener(this@QuizActivity)
        }

        if (quizId.isNotEmpty()) {
            loadQuestionsFromFirebase(quizId)
        } else {
            finish()
        }
    }

    private fun loadQuestionsFromFirebase(id: String) {
        val ref = FirebaseDatabase.getInstance("https://myquizapp-7c19d-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
        ref.child(id).child("questionList").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                questionModelList.clear()
                for (snap in snapshot.children) {
                    val q = snap.getValue(QuestionModel::class.java)
                    if (q != null) questionModelList.add(q)
                }
                if (questionModelList.isNotEmpty()) {
                    startTimer()
                    loadQuestions()
                }
            }
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(remainingTimeInMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeInMillis = millisUntilFinished
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60

                binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)

                val progress = (millisUntilFinished.toFloat() / totalTimeInMillis.toFloat() * 100).toInt()
                binding.progressBar.progress = progress.coerceAtMost(100)
            }

            override fun onFinish() {
                finishQuiz()
            }
        }.start()
    }

    private fun loadQuestions() {
        // Reset trạng thái cho câu hỏi mới
        selectedAnswer = ""
        isAnswerChecked = false

        if (currentQuestionIndex == questionModelList.size) {
            finishQuiz()
            return
        }

        binding.apply {
            tvQuestionCounter.text = "Question ${currentQuestionIndex + 1}/${questionModelList.size}"
            questionTextview.text = questionModelList[currentQuestionIndex].question

            val options = questionModelList[currentQuestionIndex].options
            if (options.size >= 4) {
                updateOptionText(layoutOption1, "A", options[0])
                updateOptionText(layoutOption2, "B", options[1])
                updateOptionText(layoutOption3, "C", options[2])
                updateOptionText(layoutOption4, "D", options[3])
            }
        }
        resetButtonColors()
    }

    private fun updateOptionText(optionBinding: OptionItemBinding, label: String, text: String) {
        optionBinding.optionLabel.text = label
        optionBinding.optionText.text = text
    }

    private fun resetButtonColors() {
        binding.apply {
            resetSingleOption(layoutOption1)
            resetSingleOption(layoutOption2)
            resetSingleOption(layoutOption3)
            resetSingleOption(layoutOption4)
        }
    }

    private fun resetSingleOption(optionBinding: OptionItemBinding) {
        // Reset màu nền về trắng
        optionBinding.optionCard.setCardBackgroundColor(Color.WHITE)

        // Đã xóa dòng strokeWidth gây lỗi vì CardView thường không hỗ trợ,
        // và logic của bạn cũng không cần dùng viền.

        optionBinding.checkmark.visibility = View.GONE
        optionBinding.optionLabel.setTextColor(Color.parseColor("#212121"))
        optionBinding.optionText.setTextColor(Color.parseColor("#616161"))

        optionBinding.root.visibility = View.VISIBLE
        optionBinding.root.isEnabled = true // Mở khóa nút
    }

    // --- LOGIC KIỂM TRA ĐÁP ÁN ---
    private fun checkAnswer(selectedOptionBinding: OptionItemBinding, answer: String) {
        // Nếu đã chọn rồi thì không cho chọn lại
        if (isAnswerChecked) return

        isAnswerChecked = true // Khóa không cho chọn tiếp
        selectedAnswer = answer

        val correctAnswer = questionModelList[currentQuestionIndex].correct

        if (answer == correctAnswer) {
            // 1. TRƯỜNG HỢP ĐÚNG
            score++
            highlightOption(selectedOptionBinding, true)
        } else {
            // 2. TRƯỜNG HỢP SAI
            highlightOption(selectedOptionBinding, false)
            // Tìm và hiện màu xanh cho đáp án đúng
            showCorrectAnswer(correctAnswer)
        }

        // Khóa tất cả các nút để không bấm được nữa
        disableAllOptions()
    }

    private fun highlightOption(optionBinding: OptionItemBinding, isCorrect: Boolean) {
        if (isCorrect) {
            // Màu Xanh (Đúng)
            optionBinding.optionCard.setCardBackgroundColor(Color.parseColor("#69F0AE"))
        } else {
            // Màu Đỏ (Sai)
            optionBinding.optionCard.setCardBackgroundColor(Color.parseColor("#FF5252"))
            optionBinding.optionLabel.setTextColor(Color.WHITE)
            optionBinding.optionText.setTextColor(Color.WHITE)
        }
        optionBinding.checkmark.visibility = View.VISIBLE
    }

    private fun showCorrectAnswer(correctAnswer: String) {
        binding.apply {
            val options = questionModelList[currentQuestionIndex].options
            if (options[0] == correctAnswer) highlightOption(layoutOption1, true)
            if (options[1] == correctAnswer) highlightOption(layoutOption2, true)
            if (options[2] == correctAnswer) highlightOption(layoutOption3, true)
            if (options[3] == correctAnswer) highlightOption(layoutOption4, true)
        }
    }

    private fun disableAllOptions() {
        binding.apply {
            layoutOption1.root.isEnabled = false
            layoutOption2.root.isEnabled = false
            layoutOption3.root.isEnabled = false
            layoutOption4.root.isEnabled = false
        }
    }

    override fun onClick(view: View?) {
        val currentQ = questionModelList.getOrNull(currentQuestionIndex) ?: return
        val options = currentQ.options

        when (view?.id) {
            // --- CÁC NÚT ĐÁP ÁN ---
            R.id.layout_option_1 -> checkAnswer(binding.layoutOption1, options[0])
            R.id.layout_option_2 -> checkAnswer(binding.layoutOption2, options[1])
            R.id.layout_option_3 -> checkAnswer(binding.layoutOption3, options[2])
            R.id.layout_option_4 -> checkAnswer(binding.layoutOption4, options[3])

            // --- NÚT CHỨC NĂNG ---
            R.id.btn_next -> {
                // Chỉ cho qua câu khi đã chọn đáp án
                if (!isAnswerChecked) {
                    Toast.makeText(this, "Bạn chưa chọn đáp án!", Toast.LENGTH_SHORT).show()
                    return
                }
                currentQuestionIndex++
                loadQuestions()
            }

            R.id.btn_skip -> {
                currentQuestionIndex++
                loadQuestions()
            }

            R.id.btn_5050 -> {
                if (isAnswerChecked) return

                val wrongOptionViews = mutableListOf<View>()
                if (options[0] != currentQ.correct) wrongOptionViews.add(binding.layoutOption1.root)
                if (options[1] != currentQ.correct) wrongOptionViews.add(binding.layoutOption2.root)
                if (options[2] != currentQ.correct) wrongOptionViews.add(binding.layoutOption3.root)
                if (options[3] != currentQ.correct) wrongOptionViews.add(binding.layoutOption4.root)

                wrongOptionViews.shuffle()

                if (wrongOptionViews.size >= 2) {
                    wrongOptionViews[0].visibility = View.INVISIBLE
                    wrongOptionViews[0].isEnabled = false
                    wrongOptionViews[1].visibility = View.INVISIBLE
                    wrongOptionViews[1].isEnabled = false
                    binding.btn5050.isEnabled = false
                    binding.btn5050.alpha = 0.5f
                }
            }

            R.id.btn_add_time -> {
                remainingTimeInMillis += 2 * 60 * 1000
                startTimer()
                binding.btnAddTime.isEnabled = false
                binding.btnAddTime.alpha = 0.5f
                Toast.makeText(this, "Đã cộng 2 phút!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun finishQuiz() {
        timer?.cancel()
        val totalQuestions = questionModelList.size
        val percentage = if (totalQuestions > 0) ((score.toFloat() / totalQuestions) * 100).toInt() else 0

        val dialogBinding = ScoreDialogBinding.inflate(layoutInflater)
        dialogBinding.apply {
            scoreProgressIndicator.progress = percentage
            scoreProgressText.text = "$percentage%"
            if (percentage >= 50) {
                scoreTitle.text = "Chúc mừng! Bạn đã đậu"
                scoreTitle.setTextColor(Color.BLUE)
            } else {
                scoreTitle.text = "Rất tiếc! Bạn chưa đạt"
                scoreTitle.setTextColor(Color.RED)
            }
            scoreSubtitle.text = "Bạn trả lời đúng $score / $totalQuestions câu"
            finishBtn.setOnClickListener { finish() }
        }
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .show()
    }
}