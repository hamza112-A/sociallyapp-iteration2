package com.mudassarkhalid.i221072

import android.os.Bundle
import android.widget.ImageView
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity6 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main6)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Redirect to MainActivity13 when prof_navigation is clicked
        val profNav = findViewById<android.widget.ImageView>(R.id.prof_navigation)
        // Set profile image in prof_navigation from session
        val sessionManager = SessionManager(this)
        val userProfile = sessionManager.getUserProfile() ?: ""
        if (userProfile.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(userProfile, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profNav?.setImageBitmap(bitmap)
            } catch (_: Exception) {}
        }
        profNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }

        // Redirect to MainActivity5 when home_navigation is clicked
        val homeNav = findViewById<android.widget.ImageView>(R.id.home_navigation)
        homeNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity5::class.java)
            startActivity(intent)
        }
        // Redirect to MainActivity6 when search_navigation is clicked
        val searchNav = findViewById<android.widget.ImageView>(R.id.search_navigation)
        searchNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity6::class.java)
            startActivity(intent)
        }
        findViewById<ImageView>(R.id.like_navigation)?.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            startActivity(intent)
        }
    }
}