package com.example.comeai_new

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import androidx.work.*

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            Log.d("NetworkChangeReceiver", "✅ Network connected. Triggering sync.")
            enqueueOfflineSyncWorker(context)
            fetchAndCacheMembershipIds(context)
        }
    }

    private fun enqueueOfflineSyncWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "OfflineSyncWorker",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}
