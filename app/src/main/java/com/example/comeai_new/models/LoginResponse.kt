package com.example.comeai_new.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: VolunteerData?
)

data class VolunteerData(
    @SerializedName("volunteer_name") val volunteerName: String?,
    @SerializedName("phone_number") val phoneNumber: String?
)
