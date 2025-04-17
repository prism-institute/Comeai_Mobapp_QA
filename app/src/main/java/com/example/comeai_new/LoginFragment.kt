package com.example.comeai_new

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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

    private val PREFS_NAME = "volunteer_cache"
    private val KEY_VOLUNTEERS = "volunteer_phone_numbers"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbarTitle = requireActivity().findViewById<TextView>(R.id.toolbarTitle)
        toolbarTitle?.text = "Login"

        phoneNumberEditText = view.findViewById(R.id.etPhoneNumber)
        btnLogin = view.findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            if (phoneNumber.isEmpty() || !phoneNumber.matches(Regex("\\d{10}"))) {
                showToast("Enter a valid 10-digit phone number")
                return@setOnClickListener
            }

            if (isOnline(requireContext())) {
                loginUserOnline(phoneNumber)
            } else {
                loginUserOffline(phoneNumber)
            }
        }
    }

    private fun loginUserOnline(phoneNumber: String) {
        val jsonObject = JSONObject().apply {
            put("action", "volunteer_login")
            put("phone_number", phoneNumber)
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
                            showToast("Login Successful!")

                            cacheVolunteerPhone(phoneNumber)

                            val bundle = Bundle().apply {
                                putString("volunteer_phone_number", phoneNumber)
                            }
                            findNavController().navigate(R.id.action_loginFragment_to_membershipFragment, bundle)
                        } else {
                            showToast("Invalid Credentials!")
                        }
                    } else {
                        handleErrorResponse(response.errorBody()?.string())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                }
            }
        }
    }

    private fun loginUserOffline(phoneNumber: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedSet = prefs.getStringSet(KEY_VOLUNTEERS, emptySet())

        if (cachedSet?.contains(phoneNumber) == true) {
            showToast("Offline Login Successful!")

            val bundle = Bundle().apply {
                putString("volunteer_phone_number", phoneNumber)
            }
            findNavController().navigate(R.id.action_loginFragment_to_membershipFragment, bundle)
        } else {
            showToast("No offline record found. Please login once online.")
        }
    }

    private fun cacheVolunteerPhone(phoneNumber: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_VOLUNTEERS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add(phoneNumber)
        prefs.edit().putStringSet(KEY_VOLUNTEERS, set).apply()
    }


    private fun handleErrorResponse(errorBody: String?) {
        if (!errorBody.isNullOrEmpty()) {
            try {
                val jsonObject = JSONObject(errorBody)
                val errorMessage = jsonObject.optString("message", "Login failed. Try again.")
                showToast(errorMessage)
            } catch (e: Exception) {
                showToast("Error parsing response")
            }
        } else {
            showToast("Unknown error occurred.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }
}
