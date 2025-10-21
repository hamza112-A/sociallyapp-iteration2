package com.mudassarkhalid.i221072

import android.net.Uri
import android.content.ClipData
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView
import android.widget.Toast

class MainActivity5 : AppCompatActivity() {
    // keep selected story image Uri here (if the user picked one)
    private var selectedStoryUri: Uri? = null
    private val TAG = "MainActivity5"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        enableEdgeToEdge()
        setContentView(R.layout.activity_main5)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Sign Out button logic
        val signOutBtn = findViewById<android.widget.Button>(R.id.sign_out_button)
        signOutBtn?.setOnClickListener {
            // Sign out from FirebaseAuth
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            // Clear session from SharedPreferences
            sessionManager.clearSession()
            Log.d(TAG, "Session cleared and user signed out.")
            // Double-check: block access if session is not cleared
            if (!sessionManager.isSessionActive() && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                val intent = android.content.Intent(this, MainActivity4::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // If session is not cleared, show error and force clear
                Toast.makeText(this, "Session not cleared. Please restart the app.", Toast.LENGTH_LONG).show()
                sessionManager.clearSession()
            }
        }

        // Example: Load user info from session for fast UI
        val userName = sessionManager.getUserName() ?: ""
        val userProfile = sessionManager.getUserProfile() ?: ""
        // Use userName and userProfile to populate UI as needed

        // Image picker launcher for selecting an image from gallery
        val pickImageLauncher: ActivityResultLauncher<String> = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val storyImage = findViewById<ImageView>(R.id.your_story_image)
                storyImage.setImageURI(it)
                // save the selected URI so it can be forwarded to Activity18
                selectedStoryUri = it
            }
        }

        // Launch gallery when camera ImageView is clicked
        val cameraBtn = findViewById<ImageView>(R.id.camera)
        cameraBtn.setOnClickListener {
            // open gallery to pick an image (images only)
            pickImageLauncher.launch("image/*")
        }

        // Redirect to MainActivity6 when search_navigation is clicked
        val searchNav = findViewById<ImageView>(R.id.search_navigation)
        searchNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity6::class.java)
            startActivity(intent)
        }

        // Redirect to MainActivity13 when prof_navigation is clicked
        val profNav = findViewById<ImageView>(R.id.prof_navigation)
        profNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }

        // Home button does nothing (remains on Activity5)
        val homeNav = findViewById<ImageView>(R.id.home_navigation)
        homeNav.setOnClickListener {
            // Do nothing, stay on Activity5
        }

        // Redirect to MainActivity8 when send icon is clicked
        val sendIcon = findViewById<ImageView>(R.id.send)
        sendIcon.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity8::class.java)
            startActivity(intent)
        }

        // Open story in MainActivity18 when the first story thumbnail is clicked
        val storyThumb = findViewById<ImageView>(R.id.story1_thumb)
        storyThumb?.setOnClickListener {
            Log.d(TAG, "story1_thumb clicked")
            val intent = android.content.Intent(this, MainActivity18::class.java)
            // pass the drawable resource id to Activity18 so it can show this image as the background
            intent.putExtra("story_drawable", R.drawable.download)
            startActivity(intent)
        }

        // Story 2
        val story2 = findViewById<ImageView>(R.id.story2_thumb)
        story2?.setOnClickListener {
            Log.d(TAG, "story2_thumb clicked")
            val intent = android.content.Intent(this, MainActivity18::class.java)
            intent.putExtra("story_drawable", R.drawable.c)
            startActivity(intent)
        }

        // Story 3
        val story3 = findViewById<ImageView>(R.id.story3_thumb)
        story3?.setOnClickListener {
            Log.d(TAG, "story3_thumb clicked")
            val intent = android.content.Intent(this, MainActivity18::class.java)
            intent.putExtra("story_drawable", R.drawable.download)
            startActivity(intent)
        }

        // Story 4
        val story4 = findViewById<ImageView>(R.id.story4_thumb)
        story4?.setOnClickListener {
            Log.d(TAG, "story4_thumb clicked")
            val intent = android.content.Intent(this, MainActivity18::class.java)
            intent.putExtra("story_drawable", R.drawable.e)
            startActivity(intent)
        }

        // New: open the user's own story image in Activity18 when `your_story_image` is clicked
        val yourStory = findViewById<ImageView>(R.id.your_story_image)
        yourStory?.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity18::class.java)
            if (selectedStoryUri != null) {
                // pass the picked image Uri as a Parcelable extra and grant temporary read permission
                intent.putExtra("story_uri", selectedStoryUri)
                // grant read permission explicitly via flag and attach ClipData for robustness
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.clipData = ClipData.newRawUri("story_uri", selectedStoryUri)
            } else {
                // no picked image — fall back to the packaged drawable
                intent.putExtra("story_drawable", R.drawable.a)
            }
            startActivity(intent)
        }
    }
}