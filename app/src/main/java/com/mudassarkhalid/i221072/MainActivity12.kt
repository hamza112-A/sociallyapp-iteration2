package com.mudassarkhalid.i221072

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity12 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main12)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set profile image in prof_navigation from session
        val sessionManager = SessionManager(this)
        val userProfile = sessionManager.getUserProfile() ?: ""
        val profNav = findViewById<android.widget.ImageView>(R.id.prof_navigation)
        if (userProfile.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(userProfile, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profNav?.setImageBitmap(bitmap)
            } catch (_: Exception) {}
        }

        val followingTab = findViewById<Button>(R.id.followingTab)
        val youTab = findViewById<Button>(R.id.youTab)
        followingTab.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            startActivity(intent)
            finish()
        }
        youTab.setOnClickListener {
            // Stay on Activity12
        }

        findViewById<ImageView>(R.id.like_navigation)?.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            startActivity(intent)
        }
    }
}