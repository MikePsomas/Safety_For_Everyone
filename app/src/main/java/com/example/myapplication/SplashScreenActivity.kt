package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Check if device is running API level 30 or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Use WindowInsetsController if available (API level 30+)
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Fallback for lower API levels
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Navigate to MainActivity after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
