package com.example.comeai_new 

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestionAdapter(
    private val questions: List<Question>,
    private val answers: MutableMap<Int, String>,
    private val isTranslated: Boolean                 
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    private val localAnswers = mutableMapOf<Int, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun getItemCount(): Int = questions.size

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {

        val question = questions[position]
        val questionText = if (isTranslated) question.assamese else question.english

        // Change questionId display for questions 91-96 to Q1-Q6
        val displayId = when (question.questionId) {
            in 91..96 -> "Q${question.questionId - 90}"
            else -> question.questionId.toString()
        }

        holder.tvQuestion.text = "$displayId. $questionText"

        val currentAnswer = answers[question.questionId] ?: localAnswers[question.questionId]

        // Reset button styles
        setButtonStyle(holder.btnYes, false)
        setButtonStyle(holder.btnNo, false)

        // Highlight selected answer
        when (currentAnswer) {
            "Yes" -> setButtonStyle(holder.btnYes, true)
            "No" -> setButtonStyle(holder.btnNo, true)
        }

        holder.btnYes.setOnClickListener {
            localAnswers[question.questionId] = "Yes"
            setButtonStyle(holder.btnYes, true)
            setButtonStyle(holder.btnNo, false)
        }

        holder.btnNo.setOnClickListener {
            localAnswers[question.questionId] = "No"
            setButtonStyle(holder.btnYes, false)
            setButtonStyle(holder.btnNo, true)
        }
    }

    private fun setButtonStyle(button: Button, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundColor(Color.LTGRAY)
            button.setTextColor(Color.BLACK)
        }
    }

    fun getAnswers(): Map<Int, String> = localAnswers

    inner class QuestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        val btnYes: Button = view.findViewById(R.id.btnYes)
        val btnNo: Button = view.findViewById(R.id.btnNo)
    }
}
