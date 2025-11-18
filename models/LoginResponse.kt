package com.example.comeai_new.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val message: String?,
    @SerializedName("volunteer_id") val volunteerId: Int? = null,
    @SerializedName("volunteer_data") val volunteerData: VolunteerData? = null
)

data class VolunteerData(
    @SerializedName("volunteer_name") val volunteerName: String?,
    @SerializedName("phone_number") val phoneNumber: String?
)