package com.example.comeai_new

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.comeai_new.network.ApiClient
import com.example.comeai_new.models.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LoginFragment : Fragment() {

    private lateinit var phoneNumberEditText: EditText
    private lateinit var btnLogin: Button
    private lateinit var nameEditText: EditText

    private val PREFS_NAME = "volunteer_cache"
    private val KEY_VOLUNTEERS =  "volunteer_json"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      //  enqueueOfflineSyncWorker(requireContext())
        context?.let {
            WorkManager.getInstance(it).enqueue(
                OneTimeWorkRequestBuilder<OfflineSyncWorker>().build()
            )
        }
        super.onViewCreated(view, savedInstanceState)
        val toolbarTitle = requireActivity().findViewById<TextView>(R.id.toolbarTitle)
        toolbarTitle?.text = "Login"

        phoneNumberEditText = view.findViewById(R.id.etPhoneNumber)
        nameEditText = view.findViewById(R.id.etName)
        btnLogin = view.findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()

            val name = nameEditText.text.toString().trim()

            if (phoneNumber.isEmpty() || !phoneNumber.matches(Regex("\\d{10}"))) {
                showDialog("Invalid Phone Number", "Please enter a valid 10-digit phone number.")
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                showDialog("Missing Name", "Please enter your full name.")
                return@setOnClickListener
            }

            if (isOnline(requireContext())) {
                loginUserOnline(phoneNumber, name)
            } else {
                loginUserOffline(phoneNumber)
            }

        }
    }

    private fun loginUserOnline(phoneNumber: String, name: String) {
        val jsonObject = JSONObject().apply {
            put("action", "volunteer_login")
            put("phone_number", phoneNumber)
            put("name", name)
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.login(requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse?.message == "Volunteer login successful" &&
                            loginResponse.volunteerId != null
                        ) {
                            showDialog("Login Successful", "Welcome!")

                            cacheVolunteerPhone(phoneNumber, name)

                            val bundle = Bundle().apply {
                                putString("volunteer_phone_number", phoneNumber)
                                putString("volunteer_name", name)
                            }
                            findNavController().navigate(R.id.action_loginFragment_to_membershipFragment, bundle)
                        } else {
                            showDialog("Login Failed", "Invalid Credentials. Please try again.")
                        }
                    } else {
                        handleErrorResponse(response.errorBody()?.string())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showDialog("Error", e.message ?: "Unknown Error Occurred.")
                }
            }
        }
    }

    private fun loginUserOffline(phoneNumber: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_VOLUNTEERS, "{}") ?: "{}"
        val json = JSONObject(jsonString)


        if (json.has(phoneNumber)) {
            val name = json.getString(phoneNumber)  // 👈 retrieve volunteer name by phone number

            showDialog("Offline Login Successful", "Proceeding offline.")

            val bundle = Bundle().apply {
                putString("volunteer_phone_number", phoneNumber)
                putString("volunteer_name", name)
            }
            findNavController().navigate(R.id.action_loginFragment_to_membershipFragment, bundle)
        } else {
            showDialog("Offline Login Failed", "No offline record found. Please login once online.")
        }
    }

    private fun cacheVolunteerPhone(phoneNumber: String,name: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString  = prefs.getString(KEY_VOLUNTEERS, "{}") ?: "{}"
        val json = JSONObject(jsonString)
        json.put(phoneNumber, name)
        prefs.edit().putString(KEY_VOLUNTEERS, json.toString()).apply()
    }


    private fun handleErrorResponse(errorBody: String?) {
        if (!errorBody.isNullOrEmpty()) {
            try {
                val jsonObject = JSONObject(errorBody)
                val errorMessage = jsonObject.optString("message", "Login failed. Try again.")
                showDialog("Login Failed", errorMessage)
            } catch (e: Exception) {
                showDialog("Error", "Error parsing server response.")
            }
        } else {
            showDialog("Unknown Error", "Please try again.")
        }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }
}
