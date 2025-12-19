package com.tien.quizapp

data class QuizModel(
    // Đổi 'val' thành 'var' để có thể gán ID sau khi lấy từ Firebase
    var id : String,
    var title : String,
    var subtitle : String,
    var time : String,
    var questionList : List<QuestionModel>
){
    // Constructor rỗng cho Firebase
    constructor() : this("","","","", emptyList())
}

data class QuestionModel(
    var question : String,
    var options : List<String>,
    var correct : String,
){
    // Constructor rỗng cho Firebase
    constructor() : this ("", emptyList(),"")
}