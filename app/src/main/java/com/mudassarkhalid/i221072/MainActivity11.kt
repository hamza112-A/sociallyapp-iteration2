package com.mudassarkhalid.i221072

import android.net.Uri
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide

class MainActivity11 : AppCompatActivity() {
    private val TAG = "MainActivity11"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main11)
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
                    // Resolve the id at runtime to avoid a hard compile-time dependency on R.id.post_image
                    val postImageResId = resources.getIdentifier("post_image", "id", packageName)
                    val postImageView = if (postImageResId != 0) findViewById<ImageView>(postImageResId) else null
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
                            try {
                                val intent = Intent(this, MainActivity5::class.java)
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

        // Wire add_navigation to open gallery for creating a post
        val addNav = findViewById<android.widget.ImageView>(R.id.add_navigation)
        addNav?.setOnClickListener {
            pickPostLauncher.launch("image/*")
        }

        findViewById<ImageView>(R.id.like_navigation)?.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            startActivity(intent)
        }

        val followingTab = findViewById<Button>(R.id.followingTab)
        val youTab = findViewById<Button>(R.id.youTab)
        followingTab.setOnClickListener {
            // Stay on Activity11
        }
        youTab.setOnClickListener {
            val intent = Intent(this, MainActivity12::class.java)
            startActivity(intent)
            finish()
        }
    }
}