package com.mudassarkhalid.i221072

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY_MS = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the existing activity_main layout as the splash layout (shows app logo)
        setContentView(R.layout.activity_main)

        // After a delay, navigate to MainActivity2
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity2::class.java))
            finish()
        }, SPLASH_DELAY_MS)
    }
}

