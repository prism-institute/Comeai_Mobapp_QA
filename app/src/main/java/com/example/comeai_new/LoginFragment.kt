package com.example.comeai_new

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.comeai_new.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val phoneNumberEditText = view.findViewById<EditText>(R.id.etPhoneNumber)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnNewHousehold = view.findViewById<Button>(R.id.btnNewHousehold)

        btnLogin.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            if (phoneNumber.isEmpty() || phoneNumber.length != 10 || !phoneNumber.matches("\\d{10}".toRegex())) {
                showToast("Enter a valid 10-digit phone number")
                return@setOnClickListener
            }


            Log.d("LoginFragment", "Phone Number Entered: $phoneNumber")

            val jsonObject = JSONObject().apply {
                put("action", "login")
                put("phone_number", phoneNumber)
            }

            val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

            loginUser(requestBody)
        }

        btnNewHousehold.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }
    }

    private fun loginUser(requestBody: okhttp3.RequestBody) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.login(requestBody)

                withContext(Dispatchers.Main) {
                    Log.d("LoginFragment", "Response Code: ${response.code()}")
                    Log.d("LoginFragment", "Raw Response: ${response.raw()}")

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        Log.d("LoginFragment", "Parsed LoginResponse: $loginResponse")

                        if (loginResponse?.message == "Login successful" && loginResponse.data != null) {
                            showToast("Login Successful!")
                            navigateToNextScreen()
                        } else {
                            showToast("Invalid Credentials!")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("LoginFragment", "Error JSON: $errorBody")
                        handleErrorResponse(errorBody)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("LoginFragment", "Exception: ${e.message}")
                    showToast("Exception: ${e.message}")
                }
            }
        }
    }


    private fun handleErrorResponse(errorBody: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
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
    }

    private fun showToast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToNextScreen() {
        lifecycleScope.launch(Dispatchers.Main) {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }
    }
}
