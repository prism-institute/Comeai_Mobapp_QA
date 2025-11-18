package com.example.comeai_new

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.appcompat.widget.Toolbar
import androidx.work.*

import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private  var networkReceiver: NetworkChangeReceiver ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (isOnline(this)) {
            fetchAndCacheMembershipIds(this)
        }
        enqueueOfflineSyncWorkerIfNetworkAvailable()
        // Use custom toolbar with logo
        val toolbar = layoutInflater.inflate(R.layout.toolbar_custom, null) as Toolbar
        setSupportActionBar(toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        setupActionBarWithNavController(navController)

        // Enable up button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }
    override fun onStart() {
        super.onStart()
        // Dynamically register for connectivity changes
        val filter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        networkReceiver = NetworkChangeReceiver()
        registerReceiver(networkReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        networkReceiver?.let {
            unregisterReceiver(it)
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    private fun enqueueOfflineSyncWorkerIfNetworkAvailable() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "OfflineSyncWorker",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }


    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}
