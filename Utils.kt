package com.example.comeai_new

import android.content.Context
import android.util.Log
import com.example.comeai_new.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
fun fetchAndCacheMembershipIds(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val requestJson = JSONObject().apply {
                put("action", "get_all_membership_ids")
            }

            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val response = ApiClient.instance.checkMembership(requestBody)

            if (response.isSuccessful) {
                val responseBody = response.body()
                val rawData = responseBody?.data

                if (rawData is List<*>) {
                    val stringIds = rawData.filterIsInstance<String>()

                    Log.d("MembershipFragment", "Fetched and caching: $stringIds")

                    val prefs = context.getSharedPreferences("membership_cache", Context.MODE_PRIVATE)
                    prefs.edit().putStringSet("membership_ids", stringIds.toSet()).apply()

                    Log.d("MembershipFragment", "✅ Fetched and cached ${stringIds.size} membership IDs.")
                } else {
                    Log.w("MembershipFragment", "⚠️ Data field is not a List<String>: $rawData")
                }
            } else {
                Log.e("MembershipFragment", "❌ Request failed - code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("MembershipFragment", "❗ Exception while fetching: ${e.message}")
        }
    }
}
