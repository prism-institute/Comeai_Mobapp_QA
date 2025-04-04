package com.example.comeai_new

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.comeai_new.models.RegisterResponse
import com.example.comeai_new.network.ApiClient
import com.example.comeai_new.network.RegisterRequest
import com.example.comeai_new.network.Member
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double? = null
    private var longitude: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val cityVillage = view.findViewById<EditText>(R.id.etCityVillage)
        val pincode = view.findViewById<EditText>(R.id.etPincode)
        val numPeople = view.findViewById<EditText>(R.id.etNumPeople)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val dynamicFieldsContainer = view.findViewById<LinearLayout>(R.id.dynamicFieldsContainer)

        numPeople.setOnEditorActionListener { _, _, _ ->
            dynamicFieldsContainer.removeAllViews()
            val count = numPeople.text.toString().toIntOrNull() ?: 0

            for (i in 1..count) {
                val ageInput = EditText(requireContext()).apply {
                    hint = "Age of Member $i"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
                dynamicFieldsContainer.addView(ageInput)

                val genderSpinner = Spinner(requireContext()).apply {
                    adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("Male", "Female", "Other")
                    )
                }
                dynamicFieldsContainer.addView(genderSpinner)
            }
            true
        }

        getLocation()

        btnRegister.setOnClickListener {
            val peopleCount = numPeople.text.toString().toIntOrNull() ?: 0
            val members = mutableListOf<Member>()

            for (i in 0 until dynamicFieldsContainer.childCount step 2) {
                val ageInput = dynamicFieldsContainer.getChildAt(i) as EditText
                val genderSpinner = dynamicFieldsContainer.getChildAt(i + 1) as Spinner

                val age = ageInput.text.toString().toIntOrNull() ?: 0
                val gender = genderSpinner.selectedItem.toString()

                if (age <= 0) {
                    Toast.makeText(requireContext(), "Please enter valid ages.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                members.add(Member(age, gender))
            }

            // Validate input fields
            if (cityVillage.text.isNullOrEmpty() || pincode.text.isNullOrEmpty() || peopleCount == 0 || members.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(
                action = "register",
                city_village = cityVillage.text.toString(),
                pincode = pincode.text.toString(),
                num_people = peopleCount,
                members = members
            )

            val jsonRequest = Gson().toJson(request)
            Log.d("ApiService", "Register Request: $jsonRequest")

            // Show progress bar while registering
            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val call = ApiClient.instance.register(request)
                    Log.d("ApiService", "API URL: ${call.request().url}")

                    val response = call.execute()

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true

                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Registered successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToQuestionnaireFragment())
                        } else {
                            Log.e("ApiService", "Error: ${response.errorBody()?.string()}")
                            Toast.makeText(requireContext(), "Registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                latitude = it.latitude
                longitude = it.longitude
                Toast.makeText(requireContext(), "Location Captured", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Location error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
