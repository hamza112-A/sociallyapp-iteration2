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
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.res.ResourcesCompat

class MainActivity5 : AppCompatActivity() {
    // keep selected story image Uri here (if the user picked one)
    private var selectedStoryUri: Uri? = null
    // keep the Base64 payload of the currently-loaded story (from Firestore) so we can pass it
    // to the full-screen activity when no Uri is available (e.g. after app restart)
    private var selectedStoryBase64: String? = null
    private val TAG = "MainActivity5"

    // Listener for realtime updates to posts
    private var postListener: com.google.firebase.firestore.ListenerRegistration? = null

    // RecyclerView + adapter to show multiple posts (current user's posts first)
    private lateinit var postsRecycler: RecyclerView
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Session check: if not active, redirect to login
        val sessionManager = SessionManager(this)
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (!sessionManager.isSessionActive() || firebaseUser == null) {
            val intent = Intent(this, MainActivity4::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

        // Initialize posts RecyclerView and adapter
        postsRecycler = findViewById(R.id.posts_recycler)
        postAdapter = PostAdapter(mutableListOf())
        postsRecycler.layoutManager = LinearLayoutManager(this)
        postsRecycler.adapter = postAdapter

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
                val intent = Intent(this, MainActivity4::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // If session is not cleared, show error and force clear
                Toast.makeText(this, "Session not cleared. Please restart the app.", Toast.LENGTH_LONG).show()
                sessionManager.clearSession()
            }
        }

        // Example: Load user info from session for fast UI (profile used below)
        val userProfile = sessionManager.getUserProfile() ?: ""
        // Use userProfile to populate UI as needed

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

                    // store locally so click handler can pass it if Uri isn't available later
                    selectedStoryBase64 = imageBase64

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

        // --- NEW: post picker / upload flow ---
        // This launcher is used when user clicks the bottom `add_navigation` ImageView to create a post.
        val pickPostLauncher: ActivityResultLauncher<String> = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                // No optimistic single-image preview here — the RecyclerView will display posts once
                // Firestore confirms the new document. We still process & upload the image here.

                // Read, resize, compress, encode to Base64 and upload to Firestore
                try {
                    val inputStream = contentResolver.openInputStream(it)
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    val maxDim = 2048 // allow a larger size for post images
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
                    // use JPEG and moderate quality to keep size reasonable
                    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val imageBytes = outputStream.toByteArray()
                    val imageBase64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)

                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserId.isNullOrEmpty()) {
                        Toast.makeText(this, "You must be logged in to upload a post.", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Upload blocked: userId is null or empty.")
                        return@let
                    }

                    // include uploader username and profile in the post document
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
                    // Add a new post document (auto-id)
                    db.collection("posts")
                        .add(postMap)
                        .addOnSuccessListener { docRef ->
                            Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Post uploaded id=${docRef.id} user=$currentUserId ts=$timestamp")
                            // Firestore listener updates the RecyclerView; scroll to top as a UX hint
                            try { postsRecycler.scrollToPosition(0) } catch (_: Exception) {}
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
        // --- end new post flow ---

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
                            // store Base64 locally so click handler can pass it if needed
                            selectedStoryBase64 = imageBase64
                            val imageBytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
                            Glide.with(this)
                                .asBitmap()
                                .load(imageBytes)
                                .circleCrop()
                                .into(yourStoryImage)
                        }
                    } else {
                        // Show profile photo as default story
                        // clear any stored story Base64 if expired or missing
                        selectedStoryBase64 = null
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
                    selectedStoryBase64 = null
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

        // New: open gallery to create a post when the bottom `add_navigation` is clicked
        val addNav = findViewById<ImageView>(R.id.add_navigation)
        addNav?.setOnClickListener {
            // open gallery to pick a post image
            pickPostLauncher.launch("image/*")
        }

        // Redirect to MainActivity6 when search_navigation is clicked
        val searchNav = findViewById<ImageView>(R.id.search_navigation)
        searchNav.setOnClickListener {
            val intent = Intent(this, MainActivity6::class.java)
            startActivity(intent)
        }

        // Redirect to MainActivity13 when prof_navigation is clicked
        profNav.setOnClickListener {
            val intent = Intent(this, MainActivity13::class.java)
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
            val intent = Intent(this, MainActivity8::class.java)
            startActivity(intent)
        }

        // Open story in MainActivity18 when the first story thumbnail is clicked
        val storyThumb = findViewById<ImageView>(R.id.story1_thumb)
        storyThumb?.setOnClickListener {
            Log.d(TAG, "story1_thumb clicked")
            val intent = Intent(this, MainActivity18::class.java)
            // pass the drawable resource id to Activity18 so it can show this image as the background
            intent.putExtra("story_drawable", R.drawable.download)
            startActivity(intent)
        }

        // Story 2
        val story2 = findViewById<ImageView>(R.id.story2_thumb)
        story2?.setOnClickListener {
            Log.d(TAG, "story2_thumb clicked")
            val intent = Intent(this, MainActivity18::class.java)
            intent.putExtra("story_drawable", R.drawable.c)
            startActivity(intent)
        }

        // Story 3
        val story3 = findViewById<ImageView>(R.id.story3_thumb)
        story3?.setOnClickListener {
            Log.d(TAG, "story3_thumb clicked")
            val intent = Intent(this, MainActivity18::class.java)
            intent.putExtra("story_drawable", R.drawable.download)
            startActivity(intent)
        }

        // Story 4
        val story4 = findViewById<ImageView>(R.id.story4_thumb)
        story4?.setOnClickListener {
            Log.d(TAG, "story4_thumb clicked")
            val intent = Intent(this, MainActivity18::class.java)
            intent.putExtra("story_drawable", R.drawable.e)
            startActivity(intent)
        }

        // Dynamically load stories from Firestore (decode Base64, show username)
        fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

        fun createStoryView(userId: String, username: String, imageBase64: String?): android.view.View {
            val container = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                val params = android.widget.LinearLayout.LayoutParams(dpToPx(70), android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                layoutParams = params
                setPadding(0, 0, dpToPx(12), 0)
            }

            // keep userId on the view for future reference (avoids unused-parameter warning)
            container.tag = userId

            val imageView = ImageView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(dpToPx(60), dpToPx(60))
                setBackgroundResource(R.drawable.circle_background)
                clipToOutline = true
                foreground = ResourcesCompat.getDrawable(resources, R.drawable.circle_stroke, theme)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            val textView = android.widget.TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                text = username
                textSize = 12f
                setTextColor(resources.getColor(android.R.color.black, theme))
                setPadding(0, dpToPx(4), 0, 0)
            }

            // Load image: prefer Base64 bytes (we use Glide for robustness)
            if (!imageBase64.isNullOrEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
                    Glide.with(this)
                        .asBitmap()
                        .load(imageBytes)
                        .circleCrop()
                        .into(imageView)
                } catch (_: Exception) {
                    imageView.setImageResource(R.drawable.a)
                }
            } else {
                imageView.setImageResource(R.drawable.a)
            }

            container.addView(imageView)
            container.addView(textView)

            // Click listener: open MainActivity18 and pass Base64 (or drawable fallback)
            container.setOnClickListener {
                val intent = Intent(this, MainActivity18::class.java)
                if (!imageBase64.isNullOrEmpty()) {
                    intent.putExtra("story_base64", imageBase64)
                } else {
                    intent.putExtra("story_drawable", R.drawable.a)
                }
                startActivity(intent)
            }

            return container
        }

        fun loadStories() {
            val storiesContainer = try {
                // HorizontalScrollView contains a single LinearLayout child — access it dynamically
                val hv = findViewById<android.widget.HorizontalScrollView>(R.id.stories_section)
                hv.getChildAt(0) as? android.widget.LinearLayout
            } catch (_: Exception) {
                null
            } ?: return

            // Keep the first child (Your Story) and remove any others before repopulating
            if (storiesContainer.childCount > 1) {
                storiesContainer.removeViews(1, storiesContainer.childCount - 1)
            }

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()
            db.collection("stories")
                .whereGreaterThan("expirationTime", now)
                .get()
                .addOnSuccessListener { snaps ->
                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                    // Build list of story entries
                    val storyEntries = mutableListOf<Triple<String, Long, String?>>() // userId, timestamp, base64
                    for (doc in snaps.documents) {
                        val uid = doc.id
                        val ts = doc.getLong("timestamp") ?: 0L
                        val base64 = doc.getString("imageBase64")
                        if (!base64.isNullOrEmpty()) {
                            storyEntries.add(Triple(uid, ts, base64))
                        }
                    }

                    // Sort: logged-in user first (if exists), then by timestamp desc
                    storyEntries.sortWith(compareByDescending<Triple<String, Long, String?>> { it.first == currentUserId }
                        .thenByDescending { it.second })

                    // For each entry, create a placeholder view in the correct order, then fetch username
                    for ((uid, _, base64) in storyEntries) {
                        // create placeholder with a short id-like username; add immediately to preserve order
                        val view = createStoryView(uid, uid.take(8), base64)
                        storiesContainer.addView(view)

                        // asynchronously fetch the actual username and update the placeholder's TextView
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { userDoc ->
                                val username = userDoc?.getString("username") ?: uid.take(8)
                                // view is created as a LinearLayout (a ViewGroup). Cast safely before accessing children.
                                val tv = (view as? android.view.ViewGroup)?.getChildAt(1) as? android.widget.TextView
                                tv?.text = username
                            }
                            .addOnFailureListener {
                                // keep the placeholder username
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to load stories: ${e.localizedMessage}")
                }
        }

        // Load dynamic stories
        loadStories()

        // New: open the user's own story image in Activity18 when `your_story_image` is clicked
        val yourStory = findViewById<ImageView>(R.id.your_story_image)
        yourStory?.setOnClickListener {
            val intent = Intent(this, MainActivity18::class.java)
            if (selectedStoryUri != null) {
                // pass the picked image Uri as a Parcelable extra and grant temporary read permission
                intent.putExtra("story_uri", selectedStoryUri)
                // grant read permission explicitly via flag and attach ClipData for robustness
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.clipData = ClipData.newRawUri("story_uri", selectedStoryUri)
            } else if (!selectedStoryBase64.isNullOrEmpty()) {
                // If we have the Firestore Base64 payload, pass it so Activity18 can show the actual image
                intent.putExtra("story_base64", selectedStoryBase64)
            } else {
                // no picked image — fall back to the packaged drawable
                intent.putExtra("story_drawable", R.drawable.a)
            }
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.like_navigation)?.setOnClickListener {
            val intent = Intent(this, MainActivity11::class.java)
            startActivity(intent)
        }

        // Start listening for posts (current user's posts will be shown first)
        startPostsListener()
    }

    override fun onResume() {
        super.onResume()
        // refresh latest post when returning to Activity5
        // (listener will also keep this up to date; ensure listener is active)
        startPostsListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        // detach Firestore listener to avoid leaks
        postListener?.remove()
        postListener = null
    }

    /**
     * Start a realtime listener for posts and update the RecyclerView. The logic collects all posts
     * (ordered by timestamp desc from Firestore), then locally partitions them into the current
     * user's posts (shown first) and others. Each partition is kept sorted by timestamp desc.
     */
    private fun startPostsListener() {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUserId = firebaseAuth.currentUser?.uid

        // remove previous listener if any
        postListener?.remove()
        postListener = null

        val q = db.collection("posts").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        postListener = q.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Posts listener error: ${'$'}{error.localizedMessage}")
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val allPosts = snapshot.documents.map { PostAdapter.fromDocument(it) }

            val (mine, others) = if (!currentUserId.isNullOrEmpty()) {
                allPosts.partition { it.userId == currentUserId }
            } else {
                Pair(emptyList(), allPosts)
            }

            val sortedMine = mine.sortedByDescending { it.timestamp }
            val sortedOthers = others.sortedByDescending { it.timestamp }

            val combined = sortedMine + sortedOthers

            runOnUiThread {
                postAdapter.setPosts(combined)
            }
        }
    }

}
