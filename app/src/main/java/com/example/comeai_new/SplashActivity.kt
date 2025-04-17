package com.example.comeai_new

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // References
        val mainLogo = findViewById<ImageView>(R.id.mainLogo)
        val poweredByText = findViewById<TextView>(R.id.poweredByText)
        val companyLogo = findViewById<ImageView>(R.id.companyLogo)

        // Load animation
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Start animations
        mainLogo.startAnimation(fadeIn)
        poweredByText.startAnimation(fadeIn)
        companyLogo.startAnimation(fadeIn)

        // Navigate to MainActivity after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }
}
