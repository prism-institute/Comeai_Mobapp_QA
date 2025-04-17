package com.example.comeai_new.models
data class RegisterHouseholdResponse(
    val message: String?,
    val membership_id: String?,
    val latitude: Double? = null,
    val longitude: Double? = null
)
