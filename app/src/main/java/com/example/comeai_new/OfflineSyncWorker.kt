package com.example.comeai_new

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.comeai_new.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class OfflineSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var hadFailure = false
        var hadSuccess = false

        try {
            // === SYNC OFFLINE QUESTIONNAIRE RESPONSES ===
            val responseFile = File(applicationContext.filesDir, "offline_responses.json")
            if (responseFile.exists()) {
                val responseArray = JSONArray(responseFile.readText())

                for (i in 0 until responseArray.length()) {
                    val json = responseArray.getJSONObject(i)

                    val wrappedPayload = JSONObject().apply {
                        put("action", "submit_questionnaire")
                        put("membership_id", json.optString("membership_id", ""))
                        put("phone_number", json.optString("phone_number", ""))
                        put("responses", json.optJSONArray("responses") ?: JSONArray())
                    }

                    Log.d("OfflineSyncWorker", "Sending questionnaire payload: $wrappedPayload")

                    val body = wrappedPayload.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val result = ApiClient.instance.submitResponse(body)
                    if (result.isSuccessful) {
                        hadSuccess = true
                        Log.d("OfflineSyncWorker", "Synced questionnaire for: ${json.optString("membership_id")}")
                    } else {
                        hadFailure = true
                        Log.e("OfflineSyncWorker", "Failed to sync response: ${result.code()}")
                    }
                }

                if (!hadFailure) responseFile.delete()
            }

            // === SYNC OFFLINE HOUSEHOLD REGISTRATIONS ===
            val regFile = File(applicationContext.filesDir, "offline_registers.json")
            if (regFile.exists()) {
                val regArray = JSONArray(regFile.readText())

                for (i in 0 until regArray.length()) {
                    val regJson = regArray.getJSONObject(i)

                    Log.d("OfflineSyncWorker", "Sending registration payload: $regJson")

                    val regBody = regJson.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val regResponse = ApiClient.instance.registerOfflineHousehold(regBody)
                    if (regResponse.isSuccessful) {
                        hadSuccess = true
                        Log.d("OfflineSyncWorker", "Synced registration for: ${regJson.optString("membership_id")}")
                    } else {
                        hadFailure = true
                        Log.e("OfflineSyncWorker", "Failed to sync registration: ${regResponse.code()}")
                    }
                }

                if (!hadFailure) regFile.delete()
            }

        } catch (e: Exception) {
            Log.e("OfflineSyncWorker", "Exception during sync: ${e.message}")
            return@withContext Result.retry()
        }

        return@withContext when {
            hadFailure && hadSuccess -> Result.retry() // partial failure
            hadFailure -> Result.retry()
            hadSuccess -> Result.success()
            else -> Result.success() // no data to sync
        }
    }
}
