package com.mudassarkhalid.i221072

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Toast

class MainActivity13 : AppCompatActivity() {
    private val TAG = "MainActivity13"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main13)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- post picker / upload flow ---
        val pickPostLauncher: ActivityResultLauncher<String> = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                try {
                    val postImageView = findViewById<android.widget.ImageView>(R.id.post_image)
                    if (postImageView != null) {
                        com.bumptech.glide.Glide.with(this)
                            .load(it)
                            .centerCrop()
                            .into(postImageView)
                    }
                } catch (_: Exception) {}

                try {
                    val inputStream = contentResolver.openInputStream(it)
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    val maxDim = 2048
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scale = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height)
                    val resizedBitmap = if (scale < 1) {
                        android.graphics.Bitmap.createScaledBitmap(
                            originalBitmap,
                            (width * scale).toInt(),
                            (height * scale).toInt(),
                            true
                        )
                    } else {
                        originalBitmap
                    }

                    val outputStream = java.io.ByteArrayOutputStream()
                    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val imageBytes = outputStream.toByteArray()
                    val imageBase64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)

                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserId.isNullOrEmpty()) {
                        Toast.makeText(this, "You must be logged in to upload a post.", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Upload blocked: userId is null or empty.")
                        return@let
                    }

                    val timestamp = System.currentTimeMillis()
                    val postMap = hashMapOf(
                        "userId" to currentUserId,
                        "imageBase64" to imageBase64,
                        "timestamp" to timestamp
                    )
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("posts")
                        .add(postMap)
                        .addOnSuccessListener { docRef ->
                            Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Post uploaded id=${docRef.id} user=$currentUserId ts=$timestamp")
                            try {
                                val intent = android.content.Intent(this, MainActivity5::class.java)
                                intent.putExtra("post_image_base64", imageBase64)
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to launch MainActivity5 after upload: ${e.localizedMessage}")
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to upload post: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            Log.e(TAG, "Failed to upload post: ${e.localizedMessage}")
                        }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing post image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error processing post image: ${e.localizedMessage}")
                }
            }
        }
        // --- end post flow ---

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
        // Redirect to MainActivity11 when like_navigation is clicked
        findViewById<android.widget.ImageView>(R.id.like_navigation)?.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity11::class.java)
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

        // Wire add_navigation to open gallery for creating a post
        val addNav = findViewById<android.widget.ImageView>(R.id.add_navigation)
        addNav?.setOnClickListener {
            pickPostLauncher.launch("image/*")
        }
    }
}