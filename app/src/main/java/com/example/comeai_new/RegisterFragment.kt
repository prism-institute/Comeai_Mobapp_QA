package com.example.comeai_new

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.comeai_new.models.RegisterHouseholdRequest
import com.example.comeai_new.models.RegisterHouseholdResponse
import com.example.comeai_new.network.ApiClient
import com.example.comeai_new.network.Member
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.File

class RegisterFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double? = null
    private var longitude: Double? = null
    private val membershipPrefs = "membership_cache"
    private val membershipKey = "membership_ids"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        requireActivity().findViewById<TextView>(R.id.toolbarTitle)?.text = "New Household"

        val membershipId = view.findViewById<EditText>(R.id.etMembershipId)
        val cityVillage = view.findViewById<EditText>(R.id.etCityVillage)
        val pincode = view.findViewById<EditText>(R.id.etPincode)
        val numPeople = view.findViewById<EditText>(R.id.etNumPeople)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val dynamicFieldsContainer = view.findViewById<LinearLayout>(R.id.dynamicFieldsContainer)

        val volunteerPhoneNumber = arguments?.getString("volunteer_phone_number") ?: return

        numPeople.setOnEditorActionListener { _, _, _ ->
            dynamicFieldsContainer.removeAllViews()
            val count = numPeople.text.toString().toIntOrNull() ?: 0

            for (i in 1..count) {
                val ageInput = EditText(requireContext()).apply {
                    hint = "Age of Member $i"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
                val genderSpinner = Spinner(requireContext()).apply {
                    adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Male", "Female", "Other"))
                }
                dynamicFieldsContainer.addView(ageInput)
                dynamicFieldsContainer.addView(genderSpinner)
            }
            true
        }

        getLocation()

        btnRegister.setOnClickListener {
            val memId = membershipId.text.toString().trim()
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

            if (memId.isEmpty() || cityVillage.text.isNullOrEmpty() || pincode.text.isNullOrEmpty() || members.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hasAdult = members.any { it.age >= 18 }
            if (!hasAdult) {
                Toast.makeText(requireContext(), "At least one member must be 18 or older.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("LocationCheck", "Latitude: $latitude, Longitude: $longitude")
            val request = RegisterHouseholdRequest(
                membership_id = memId,
                city_village = cityVillage.text.toString(),
                pincode = pincode.text.toString(),
                members = members,
                phone_number = volunteerPhoneNumber,
                latitude = latitude,
                longitude=longitude
            )

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            lifecycleScope.launch {
                if (isOnline()) {
                    registerOnline(request, progressBar, btnRegister)
                } else {
                    if (isDuplicateMembershipOffline(memId)) {
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true
                        Toast.makeText(requireContext(), "Membership ID already exists (offline).", Toast.LENGTH_SHORT).show()
                    } else {
                        saveOfflineRequest(request)
                        cacheMembershipId(memId)
                        Toast.makeText(requireContext(), "Saved offline", Toast.LENGTH_SHORT).show()
                        navigateToQuestionnaire(memId, volunteerPhoneNumber)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isOnline()) syncOfflineRegistrations()
    }

    private suspend fun registerOnline(request: RegisterHouseholdRequest, progressBar: ProgressBar, btn: Button) {
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.registerHousehold(request).execute()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btn.isEnabled = true
                    if (response.isSuccessful) {
                        cacheMembershipId(request.membership_id)
                        Toast.makeText(requireContext(), "Registered: ${request.membership_id}", Toast.LENGTH_SHORT).show()
                        navigateToQuestionnaire(request.membership_id, request.phone_number)
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btn.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveOfflineRequest(request: RegisterHouseholdRequest) {
        val file = File(requireContext().filesDir, "offline_registers.json")
        val existing = if (file.exists()) JSONArray(file.readText()) else JSONArray()
        existing.put(JSONObject(Gson().toJson(request)))
        file.writeText(existing.toString())
    }

    private fun isDuplicateMembershipOffline(memId: String): Boolean {
        val file = File(requireContext().filesDir, "offline_registers.json")
        if (!file.exists()) return false

        val existing = JSONArray(file.readText())
        for (i in 0 until existing.length()) {
            val item = existing.getJSONObject(i)
            if (item.optString("membership_id") == memId) return true
        }
        return false
    }

    private fun syncOfflineRegistrations() {
        val file = File(requireContext().filesDir, "offline_registers.json")
        if (!file.exists()) return

        try {
            val array = JSONArray(file.readText())
            lifecycleScope.launch(Dispatchers.IO) {
                for (i in 0 until array.length()) {
                    val json = array.getJSONObject(i)
                    val request = Gson().fromJson(json.toString(), RegisterHouseholdRequest::class.java)
                    val response = ApiClient.instance.registerHousehold(request).execute()
                    if (response.isSuccessful) cacheMembershipId(request.membership_id)
                }
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("RegisterFragment", "Sync failed: ${e.message}")
        }
    }

    private fun cacheMembershipId(memId: String) {
        val prefs = requireContext().getSharedPreferences(membershipPrefs, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(membershipKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        ids.add(memId)
        prefs.edit().putStringSet(membershipKey, ids).apply()
    }

    private fun navigateToQuestionnaire(membershipId: String, phoneNumber: String) {
        val bundle = Bundle().apply {
            putString("membership_id", membershipId)
            putString("phone_number", phoneNumber)
        }
        findNavController().navigate(R.id.action_registerFragment_to_questionnaireFragment, bundle)
    }

    private fun isOnline(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
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
            }
        }
    }
}

