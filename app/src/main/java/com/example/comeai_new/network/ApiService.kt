package com.example.comeai_new.network

import com.example.comeai_new.models.LoginResponse
import com.example.comeai_new.models.RegisterResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
data class Member(
    val age: Int,
    val gender: String
)



// Updated RegisterRequest with correct fields
data class RegisterRequest(
   // val household_name: String,
   // val phone_number: String,
    val city_village: String,
    //val address: String,
    val pincode: String,
    val num_people: Int,       // Number of people in household
    val members: List<Member> ,
   // val latitude: Double,      // Latitude of location
   // val longitude: Double,     // Longitude of location
    val action: String = "register"
)

// Updated LoginRequest (unchanged)
data class LoginRequest(

    val phone_number: String,
    val action: String = "login"
)

// Standardized LambdaResponse
data class LambdaResponse(
    val statusCode: Int,
    val body: String
)

// Define ResponseBody if needed
data class ResponseBody(
    val message: String,
    val data: Any? = null
)

interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse> // Changed Response type

    @Headers("Content-Type: application/json")
    @POST("login")
    suspend fun login(@Body requestBody: RequestBody): Response<LoginResponse>
}


