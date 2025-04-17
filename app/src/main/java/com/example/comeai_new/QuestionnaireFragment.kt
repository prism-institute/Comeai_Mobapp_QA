package com.example.comeai_new

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.comeai_new.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class QuestionnaireFragment : Fragment(R.layout.fragment_questionnaire) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnTranslate: Button
    private lateinit var questionAdapter: QuestionAdapter

    private var questions: List<Question> = listOf()
    private var currentPage = 0
    private val pageSize = 10
    private var isTranslated = false // default language is English
    private val answers = mutableMapOf<Int, String>()  // questionId to response

    private var membershipId: String? = null
    private var phoneNumber: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<TextView>(R.id.toolbarTitle)?.text = "Questionnaire"
        recyclerView = view.findViewById(R.id.recyclerViewQuestions)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnTranslate = view.findViewById(R.id.btnTranslate)

        membershipId = arguments?.getString("membership_id")
        phoneNumber = arguments?.getString("phone_number")

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadQuestionsFromAssets()
        setupPagination()

        btnPrev.setOnClickListener {
            saveCurrentAnswers()
            if (currentPage > 0) {
                currentPage--
                setupPagination()
            }
        }

        btnNext.setOnClickListener {
            saveCurrentAnswers()
            if ((currentPage + 1) * pageSize < questions.size) {
                currentPage++
                setupPagination()
            }
        }

        btnTranslate.setOnClickListener {
            isTranslated = !isTranslated
            btnTranslate.text = if (isTranslated) "Translate to English" else "অসমীয়ালৈ অনুবাদ কৰক"
            setupPagination()
        }

        btnSubmit.setOnClickListener {
            saveCurrentAnswers()
            lifecycleScope.launch {
                if (isOnline(requireContext())) {
                    sendAnswersToBackend()
                } else {
                    storeAnswersOffline()
                }


                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Responses saved!", Toast.LENGTH_SHORT).show()
                    val bundle = Bundle().apply {
                        putString("volunteer_phone_number", phoneNumber)
                    }
                    findNavController().navigate(R.id.action_questionnaireFragment_to_membershipFragment, bundle)
                    // ✅ Navigate back to MembershipFragment
                   // findNavController().navigate(R.id.action_questionnaireFragment_to_membershipFragment)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isOnline(requireContext())) {
            sendOfflineResponsesIfAny()
        }
    }

    private fun saveCurrentAnswers() {
        val pageAnswers = questionAdapter.getAnswers()
        answers.putAll(pageAnswers)
    }

    private fun loadQuestionsFromAssets() {
        val inputStream = requireContext().assets.open("questions.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        questions = (0 until jsonArray.length()).map { i ->
            val obj = jsonArray.getJSONObject(i)
            Question(obj.getInt("question_id"), obj.getString("english"), obj.getString("assamese"))
        }
    }

    private fun setupPagination() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, questions.size)
        val pageQuestions = questions.subList(start, end)
        questionAdapter = QuestionAdapter(pageQuestions, answers, isTranslated)
        recyclerView.adapter = questionAdapter
    }

    private suspend fun sendAnswersToBackend() {
        val responseArray = JSONArray()
        for ((qid, response) in answers) {
            val obj = JSONObject().apply {
                put("question_id", qid)
                put("response", response)
            }
            responseArray.put(obj)
        }

        val jsonObject = JSONObject().apply {
            put("action", "submit_questionnaire")
            put("membership_id", membershipId ?: "")
            put("phone_number", phoneNumber ?: "")
            put("responses", responseArray)
        }

        withContext(Dispatchers.IO) {
            val body = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val result = ApiClient.instance.submitResponse(body)
            if (result.isSuccessful) {
                Log.d("QuestionnaireFragment", "Submitted successfully")
            } else {
                Log.e("QuestionnaireFragment", "Submit failed with ${result.code()}")
            }
        }
    }

    private fun storeAnswersOffline() {
        val file = File(requireContext().filesDir, "offline_responses.json")

        // Load existing responses
        val existingArray = if (file.exists()) {
            try {
                Log.d("OfflineStore", "Current offline file: ${file.readText()}")
                JSONArray(file.readText())
            } catch (e: Exception) {
                Log.e("OfflineStore", "Failed to parse existing file: ${e.message}")
                JSONArray()
            }
        } else {
            Log.d("OfflineStore", "offline_responses.json doesn't exist yet. Creating new.")
            JSONArray()
        }

        val responseObj = JSONObject().apply {
            put("membership_id", membershipId ?: "")
            put("phone_number", phoneNumber ?: "")

            val responseArray = JSONArray()
            for ((qid, response) in answers) {
                val answerObj = JSONObject().apply {
                    put("question_id", qid)
                    put("response", response)
                }
                responseArray.put(answerObj)
            }
            put("responses", responseArray)
        }

        existingArray.put(responseObj)

        try {
            file.writeText(existingArray.toString())
            Log.d("OfflineStore", "Saved offline response for $membershipId")
        } catch (e: Exception) {
            Log.e("OfflineStore", "Error saving offline: ${e.message}")
        }
    }




    private fun sendOfflineResponsesIfAny() {
        val file = File(requireContext().filesDir, "offline_responses.json")

        if (!file.exists()) {
            Log.d("OfflineSync", "No offline_responses.json file found.")
            return
        }

        try {
            // ✅ Ensure valid JSON structure even if file is empty
            val jsonString = file.readText().ifBlank { "[]" }
            val jsonArray = JSONArray(jsonString)

            if (jsonArray.length() == 0) {
                Log.d("OfflineSync", "offline_responses.json exists but is empty.")
                return
            }

            lifecycleScope.launch(Dispatchers.IO) {
                var allSynced = true

                for (i in 0 until jsonArray.length()) {
                    val entry = jsonArray.getJSONObject(i)

                    val membershipId = entry.optString("membership_id", "")
                    val phoneNumber = entry.optString("phone_number", "")
                    val responses = entry.optJSONArray("responses") ?: continue

                    val fullPayload = JSONObject().apply {
                        put("action", "submit_questionnaire")
                        put("membership_id", membershipId)
                        put("phone_number", phoneNumber)
                        put("responses", responses)
                    }

                    val body = fullPayload.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val result = ApiClient.instance.submitResponse(body)

                    if (result.isSuccessful) {
                        Log.d("OfflineSync", "Synced successfully for: $membershipId")
                    } else {
                        allSynced = false
                        val errorBody = result.errorBody()?.string()
                        Log.e("OfflineSync", "Failed syncing for: $membershipId - ${result.code()} - $errorBody")
                    }
                }

                if (allSynced) {
                    file.delete()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "All offline responses synced", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("OfflineSync", "Error parsing/sending offline data: ${e.message}")
        }
    }


    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }
}

data class Question(val questionId: Int, val english: String, val assamese: String)