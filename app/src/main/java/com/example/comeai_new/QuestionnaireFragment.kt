package com.example.comeai_new

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class QuestionnaireFragment : Fragment(R.layout.fragment_questionnaire) {

    private lateinit var questionAdapter: QuestionAdapter
    private val englishQuestions = listOf(
        "Are you able to provide three meals a day for yourself and your family?",
        "Are you able to pay for buying food and groceries that you need from the market?",
        "Do you have to travel long distances or spend long times to get to the local food market?",
        "Do you lose food to spoilage due to inadequate storage facilities?",
        "Are you able to grow a variety of crops for your own consumption?",
        "Are you able to grow a diverse variety of food crops on rotation throughout the year?",
        "Are there enough local breeds in your livestock/fish/poultry that are eaten by you/your community?",
        "Do you have frequent diarrhea and stomach-related illnesses in the community?"
    )

    private val assameseQuestions = listOf(
        "আপুনি আৰু আপোনাৰ পৰিয়ালৰ বাবে এদিনত তিনিবাৰ খাবলৈ সক্ষম নে?",
        "আপুনি বজাৰত লাগিব পৰা খাদ্য আৰু খৰিদৰ বাবে পৰিশোধ কৰিবলৈ সক্ষম নে?",
        "আপোনাক স্থানীয় খাদ্য বজাৰত যাবৰ বাবে দীঘলীয়া দূৰত্ব অতিক্ৰম কৰিব লগা হয় নে?",
        "সংগ্ৰহ ব্যৱস্থাৰ অভাৱৰ বাবে আপুনি খাদ্য নষ্ট কৰে নে?",
        "আপুনি নিজৰ বাবেই খাদ্য শস্য উলিয়াবলৈ সক্ষম নে?",
        "আপুনি বছৰৰ বিভিন্ন সময়ত খাদ্য শস্যৰ বিভিন্ন জাতি পালটাই উলিয়াবলৈ সক্ষম নে?",
        "আপোনাৰ বা আপোনাৰ সমাজৰ খাদ্যত প্ৰচলিত পৰ্যাপ্ত স্থানীয় জাতিৰ গৰু/মাছ/পোহনীয়া প্ৰাণী আছেনে?",
        "আপোনাৰ সমাজত প্ৰায়ে পেটৰ সমস্যা বা ডায়েৰিয়া হয় নে?"
    )

    private var isTranslated = false  // Flag to track the current language state

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewQuestions)
        val btnTranslate = view.findViewById<Button>(R.id.btnTranslate)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // Initialize RecyclerView with English questions
        questionAdapter = QuestionAdapter(englishQuestions)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = questionAdapter

        // Handle Translate Button Click
        btnTranslate.setOnClickListener {
            if (isTranslated) {
                questionAdapter.updateQuestions(englishQuestions)
                btnTranslate.text = "Translate to Assamese"
                isTranslated = false
            } else {
                questionAdapter.updateQuestions(assameseQuestions)
                btnTranslate.text = "Translate to English"
                isTranslated = true
            }
        }

        // Handle Logout Button Click (Implement navigation to login screen)
        btnLogout.setOnClickListener {
            // TODO: Add logic to navigate back to login
        }
    }
}
