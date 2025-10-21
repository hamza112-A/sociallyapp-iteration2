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
import com.bumptech.glide.Glide

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

        // Set profile image in prof_navigation from session
        val profNav = findViewById<ImageView>(R.id.prof_navigation)
        if (userProfile.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(userProfile, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profNav?.setImageBitmap(bitmap)
            } catch (_: Exception) {}
        }

        // Upload story to Firestore after image is picked
        val pickImageLauncher: ActivityResultLauncher<String> = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val storyImage = findViewById<ImageView>(R.id.your_story_image)
                // Use Glide to load the picked image as a circle
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(storyImage)
                selectedStoryUri = it

                // Resize and compress image before upload
                try {
                    val inputStream = contentResolver.openInputStream(it)
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    val maxDim = 1024
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
                    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val imageBytes = outputStream.toByteArray()
                    val imageBase64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)

                    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (userId.isNullOrEmpty()) {
                        Toast.makeText(this, "You must be logged in to upload a story.", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Upload blocked: userId is null or empty.")
                        return@let
                    }
                    val timestamp = System.currentTimeMillis()
                    val expirationTime = timestamp + 24 * 60 * 60 * 1000 // 24 hours in ms
                    val storyMap = hashMapOf(
                        "userId" to userId,
                        "imageBase64" to imageBase64,
                        "timestamp" to timestamp,
                        "expirationTime" to expirationTime
                    )
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("stories")
                        .document(userId)
                        .set(storyMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Story uploaded!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Story uploaded for userId=$userId at $timestamp")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to upload story: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            Log.e(TAG, "Failed to upload story: ${e.localizedMessage}")
                        }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error processing image: ${e.localizedMessage}")
                }
            }
        }

        // Load and display story (show profile photo if no story or expired)
        val yourStoryImage = findViewById<ImageView>(R.id.your_story_image)
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        if (userId != null) {
            db.collection("stories").document(userId).get()
                .addOnSuccessListener { doc ->
                    val expirationTime = doc.getLong("expirationTime") ?: 0L
                    val now = System.currentTimeMillis()
                    if (doc.exists() && expirationTime > now) {
                        val imageBase64 = doc.getString("imageBase64")
                        if (!imageBase64.isNullOrEmpty()) {
                            val imageBytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
                            Glide.with(this)
                                .asBitmap()
                                .load(imageBytes)
                                .circleCrop()
                                .into(yourStoryImage)
                        }
                    } else {
                        // Show profile photo as default story
                        val userProfile = sessionManager.getUserProfile() ?: ""
                        if (userProfile.isNotEmpty()) {
                            val imageBytes = android.util.Base64.decode(userProfile, android.util.Base64.DEFAULT)
                            Glide.with(this)
                                .asBitmap()
                                .load(imageBytes)
                                .circleCrop()
                                .into(yourStoryImage)
                        } else {
                            Glide.with(this)
                                .load(R.drawable.a)
                                .circleCrop()
                                .into(yourStoryImage)
                        }
                    }
                }
                .addOnFailureListener {
                    // On error, fallback to profile photo
                    val userProfile = sessionManager.getUserProfile() ?: ""
                    if (userProfile.isNotEmpty()) {
                        val imageBytes = android.util.Base64.decode(userProfile, android.util.Base64.DEFAULT)
                        Glide.with(this)
                            .asBitmap()
                            .load(imageBytes)
                            .circleCrop()
                            .into(yourStoryImage)
                    } else {
                        Glide.with(this)
                            .load(R.drawable.a)
                            .circleCrop()
                            .into(yourStoryImage)
                    }
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

        // Load story thumbnails as circles using Glide
        Glide.with(this)
            .load(R.drawable.download)
            .circleCrop()
            .into(findViewById<ImageView>(R.id.story1_thumb))
        Glide.with(this)
            .load(R.drawable.c)
            .circleCrop()
            .into(findViewById<ImageView>(R.id.story2_thumb))
        Glide.with(this)
            .load(R.drawable.download)
            .circleCrop()
            .into(findViewById<ImageView>(R.id.story3_thumb))
        Glide.with(this)
            .load(R.drawable.e)
            .circleCrop()
            .into(findViewById<ImageView>(R.id.story4_thumb))

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