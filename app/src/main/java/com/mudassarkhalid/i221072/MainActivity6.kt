package com.mudassarkhalid.i221072

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide

class MainActivity6 : AppCompatActivity() {
    private val TAG = "MainActivity6"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main6)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- post picker / upload flow (copied/adapted from MainActivity5) ---
        val pickPostLauncher: ActivityResultLauncher<String> = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                // Optional: show an optimistic preview if this activity has a preview ImageView named post_image
                try {
                    val postImageView = findViewById<ImageView>(R.id.post_image)
                    if (postImageView != null) {
                        Glide.with(this)
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

                    // get uploader info from session at upload time
                    val sessionManager = SessionManager(this)
                    val uploaderName = sessionManager.getUserName() ?: ""
                    val uploaderProfile = sessionManager.getUserProfile() ?: ""

                    val timestamp = System.currentTimeMillis()
                    val postMap = hashMapOf(
                        "userId" to currentUserId,
                        "imageBase64" to imageBase64,
                        "timestamp" to timestamp,
                        "username" to uploaderName,
                        "userProfile" to uploaderProfile
                    )
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("posts")
                        .add(postMap)
                        .addOnSuccessListener { docRef ->
                            Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Post uploaded id=${docRef.id} user=$currentUserId ts=$timestamp")
                            // Navigate to MainActivity5 so it can refresh the user's latest post
                            try {
                                val intent = Intent(this, MainActivity5::class.java)
                                // clear top to avoid stacking multiple copies
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                                finish()
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
            val intent = Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }

        // Redirect to MainActivity5 when home_navigation is clicked
        val homeNav = findViewById<android.widget.ImageView>(R.id.home_navigation)
        homeNav.setOnClickListener {
            val intent = Intent(this, MainActivity5::class.java)
            startActivity(intent)
        }
        // Redirect to MainActivity6 when search_navigation is clicked
        val searchNav = findViewById<android.widget.ImageView>(R.id.search_navigation)
        searchNav.setOnClickListener {
            val intent = Intent(this, MainActivity6::class.java)
            startActivity(intent)
        }
        findViewById<ImageView>(R.id.like_navigation)?.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            startActivity(intent)
        }

        // Wire add_navigation to open gallery for creating a post
        val addNav = findViewById<android.widget.ImageView>(R.id.add_navigation)
        addNav?.setOnClickListener {
            pickPostLauncher.launch("image/*")
        }
    }
}
