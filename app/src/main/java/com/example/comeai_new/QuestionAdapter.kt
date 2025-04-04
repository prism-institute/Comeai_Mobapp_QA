package com.example.comeai_new

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestionAdapter(private var questions: List<String>) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        val btnYes: Button = view.findViewById(R.id.btnYes)
        val btnNo: Button = view.findViewById(R.id.btnNo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.tvQuestion.text = questions[position]

        holder.btnYes.setOnClickListener {
            // Handle Yes Click
        }

        holder.btnNo.setOnClickListener {
            // Handle No Click
        }
    }

    override fun getItemCount(): Int {
        return questions.size
    }

    // Update questions dynamically
    fun updateQuestions(newQuestions: List<String>) {
        questions = newQuestions
        notifyDataSetChanged()
    }
}
