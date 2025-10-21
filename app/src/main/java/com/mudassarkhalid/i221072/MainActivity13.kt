package com.mudassarkhalid.i221072

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity13 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main13)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Redirect to MainActivity15 when btnEditProfile is clicked
        val editProfileBtn = findViewById<android.widget.Button>(R.id.btnEditProfile)
        editProfileBtn.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity15::class.java)
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
        // Redirect to MainActivity13 when prof_navigation is clicked
        val profNav = findViewById<android.widget.ImageView>(R.id.prof_navigation)
        profNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }

        // Session check: if not active, redirect to login
        val sessionManager = SessionManager(this)
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (!sessionManager.isSessionActive() || firebaseUser == null) {
            val intent = android.content.Intent(this, MainActivity4::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // Load user info from session for fast UI
        val userName = sessionManager.getUserName() ?: ""
        val userProfile = sessionManager.getUserProfile() ?: ""
        // Set userName and userProfile to profile UI elements
        val userNameView = findViewById<android.widget.TextView>(R.id.ProfileName)
        userNameView?.text = userName
        val profileImageView = findViewById<android.widget.ImageView>(R.id.ProfilePicture)
        if (userProfile.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(userProfile, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profileImageView?.setImageBitmap(bitmap)
            } catch (_: Exception) {}
        }

        // Set profile image in prof_navigation from session
        val userProfileNav = sessionManager.getUserProfile() ?: ""
        if (userProfileNav.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(userProfileNav, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profNav?.setImageBitmap(bitmap)
            } catch (_: Exception) {}
        }
    }
}