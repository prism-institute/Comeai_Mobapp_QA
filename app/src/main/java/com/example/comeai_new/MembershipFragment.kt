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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MembershipFragment : Fragment() {

    private var volunteerPhoneNumber: String? = null
    private val PREF_NAME = "membership_cache"
    private val KEY_MEMBERSHIP_IDS = "membership_ids"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_membership, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<TextView>(R.id.toolbarTitle)?.text = "Membership"

        volunteerPhoneNumber = arguments?.getString("volunteer_phone_number")
        Log.d("MembershipFragment", "Volunteer Phone Number: $volunteerPhoneNumber")

        val membershipIdEditText = view.findViewById<EditText>(R.id.etMembershipId)
        val btnLoginHousehold = view.findViewById<Button>(R.id.btnLoginHousehold)
        val btnRegisterHousehold = view.findViewById<Button>(R.id.btnRegisterHousehold)

        if (isOnline(requireContext())) {
            fetchAndCacheMembershipIds(requireContext())
        }

        btnLoginHousehold.setOnClickListener {
            val membershipId = membershipIdEditText.text.toString().trim()
            if (membershipId.isEmpty()) {
                showToast("Please enter Membership ID")
                return@setOnClickListener
            }

            if (isOnline(requireContext())) {
                checkMembershipOnline(membershipId)
            } else {
                checkMembershipOffline(membershipId)
            }
        }

        btnRegisterHousehold.setOnClickListener {
            val membershipId = membershipIdEditText.text.toString().trim()
            if (membershipId.isEmpty()) {
                showToast("Please enter Membership ID to register")
                return@setOnClickListener
            }

            val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val cachedSet = prefs.getStringSet(KEY_MEMBERSHIP_IDS, emptySet())
            if (cachedSet?.contains(membershipId) == true) {
                showToast("This Membership ID is already registered (cached).")
                return@setOnClickListener
            }

            val file = File(requireContext().filesDir, "offline_registers.json")
            if (file.exists()) {
                val array = JSONArray(file.readText())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.optString("membership_id") == membershipId) {
                        showToast("This Membership ID is already registered (offline).")
                        return@setOnClickListener
                    }
                }
            }

            val bundle = Bundle().apply {
                putString("volunteer_phone_number", volunteerPhoneNumber)
                putString("membership_id", membershipId)
            }
            findNavController().navigate(R.id.action_membershipFragment_to_registerFragment, bundle)
        }
    }

    private fun checkMembershipOnline(membershipId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("action", "check_membership")
                    put("membership_id", membershipId)
                }

                val requestBody = jsonObject.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val response = ApiClient.instance.getAllMembershipIds(requestBody)


                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        cacheMembershipId(membershipId)
                        val bundle = Bundle().apply {
                            putString("membership_id", membershipId)
                            putString("phone_number", volunteerPhoneNumber ?: "")
                        }
                        findNavController().navigate(R.id.action_membershipFragment_to_questionnaireFragment, bundle)
                        showToast("Membership ID valid!")
                    } else {
                        val error = response.errorBody()?.string()
                        val msg = JSONObject(error ?: "").optString("message", "Invalid Membership ID")
                        showToast(msg)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MembershipFragment", "Exception: ${e.message}")
                    showToast("Error: ${e.message}")
                }
            }
        }
    }

    private fun checkMembershipOffline(membershipId: String) {
        val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cachedSet = prefs.getStringSet(KEY_MEMBERSHIP_IDS, emptySet())

        if (cachedSet?.contains(membershipId) == true) {
            showToast("Offline: Membership ID is valid!")
            val bundle = Bundle().apply {
                putString("membership_id", membershipId)
                putString("phone_number", volunteerPhoneNumber ?: "")
            }
            findNavController().navigate(R.id.action_membershipFragment_to_questionnaireFragment, bundle)
        } else {
            showToast("Offline: Membership ID not found.")
        }
    }

    private fun cacheMembershipId(membershipId: String) {
        val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_MEMBERSHIP_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        ids.add(membershipId)
        prefs.edit().putStringSet(KEY_MEMBERSHIP_IDS, ids).apply()
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchAndCacheMembershipIds(context: Context) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject().apply {
                    put("action", "get_all_membership_ids")
                }

                val requestBody = jsonObject.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                // ✅ Correct API call for getting ALL membership IDs
                val response = ApiClient.instance.getAllMembershipIds(requestBody)

                if (response.isSuccessful) {
                    val membershipIds = response.body()?.data ?: emptyList()
                    Log.d("MembershipFragment", "Fetched and caching: $membershipIds")

                    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putStringSet(KEY_MEMBERSHIP_IDS, membershipIds.toSet()).apply()

                    Log.d("MembershipFragment", "✅ Fetched and cached ${membershipIds.size} membership IDs.")
                } else {
                    Log.e("MembershipFragment", "❌ Failed to fetch IDs - Code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MembershipFragment", "❗ Exception: ${e.message}")
            }
        }
    }
}
