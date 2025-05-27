package com.example.comeai_new.models

import com.example.comeai_new.network.Member


data class RegisterHouseholdRequest(
    val action: String = "register_household",
    val membership_id: String,
    val city_village: String,
    val pincode: String,
    val members: List<Member>,
    val phone_number: String,
    val latitude: Double? = null,                // Optional: can be passed if location captured
    val longitude: Double? = null,  // To pass volunteer's phone for validation
    val volunteer_name: String // ✅ Add this field
)