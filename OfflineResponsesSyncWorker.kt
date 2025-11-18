import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.comeai_new.models.RegisterHouseholdRequest
import com.example.comeai_new.network.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class OfflineResponseSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val file = File(applicationContext.filesDir, "offline_responses.json")
        if (!file.exists()) return Result.success()

        return try {
            val jsonArray = JSONArray(file.readText())
            for (i in 0 until jsonArray.length()) {
                val entry = jsonArray.getJSONObject(i)

                val membershipId = entry.optString("membership_id")
                val phoneNumber = entry.optString("phone_number")
                val responses = entry.optJSONArray("responses") ?: continue

                val payload = JSONObject().apply {
                    put("action", "submit_questionnaire")
                    put("membership_id", membershipId)
                    put("phone_number", phoneNumber)
                    put("responses", responses)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val result = ApiClient.instance.submitResponse(body)

                if (!result.isSuccessful) return Result.retry()
            }

            file.delete()
            Result.success()
        } catch (e: Exception) {
            Log.e("OfflineSyncWorker", "Error: ${e.message}")
            Result.retry()
        }
    }
}

class OfflineRegistrationSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val file = File(applicationContext.filesDir, "offline_registers.json")
        if (!file.exists()) return@withContext Result.success()

        try {
            val jsonArray = JSONArray(file.readText())

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val request =
                    Gson().fromJson(jsonObject.toString(), RegisterHouseholdRequest::class.java)

                val response = ApiClient.instance.registerHousehold(request).execute()
                if (!response.isSuccessful) {
                    Log.e("OfflineRegistrationSync", "Failed to sync: ${request.membership_id}")
                    return@withContext Result.retry()
                }

                Log.d("OfflineRegistrationSync", "Synced: ${request.membership_id}")
            }

            file.delete()
            Result.success()
        } catch (e: Exception) {
            Log.e("OfflineRegistrationSync", "Exception: ${e.message}")
            Result.retry()
        }
    }
}