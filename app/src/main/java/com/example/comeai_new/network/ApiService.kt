package com.example.comeai_new.network

import com.example.comeai_new.models.LoginResponse
import com.example.comeai_new.models.RegisterHouseholdRequest
import com.example.comeai_new.models.RegisterHouseholdResponse
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

// Register request model
data class RegisterRequest(
    val city_village: String,
    val pincode: String,
    val num_people: Int,
    val members: List<Member>,
    val action: String = "register"
)

// Login request model
data class LoginRequest(
    val phone_number: String,
    val action: String = "login"
)

// Generic Lambda response body
data class ResponseBody(
    val message: String,
    val data: Any? = null
)


data class MembershipListResponse(
    val message: String,
    val data: List<String>
)

interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("register")
    fun registerHousehold(@Body request: RegisterHouseholdRequest): Call<RegisterHouseholdResponse>

    @Headers("Content-Type: application/json")
    @POST("login")
    suspend fun login(@Body requestBody: RequestBody): Response<LoginResponse>

    @Headers("Content-Type: application/json")
    @POST(".")
    suspend fun checkMembership(@Body requestBody: RequestBody): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST(".")
    suspend fun submitResponse(@Body requestBody: RequestBody): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST(".")
    suspend fun registerOfflineHousehold(@Body requestBody: RequestBody): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST(".")
    suspend fun getAllMembershipIds(@Body requestBody: RequestBody): Response<MembershipListResponse>




}
