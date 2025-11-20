package com.mudassarkhalid.i221072

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class MainActivity18 : AppCompatActivity() {
    private val TAG = "MainActivity18"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main18)

        // apply system window insets as before
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the StoryBackground ImageView
        val storyBackground = findViewById<ImageView>(R.id.StoryBackground)

        // 1) Try to get a Parcelable Uri extra (preferred)
        val parcelableUri = try {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>("story_uri")
        } catch (e: Exception) {
            Log.w(TAG, "getParcelableExtra failed", e)
            null
        }

        if (parcelableUri != null) {
            Log.d(TAG, "Applying StoryBackground from Parcelable URI: $parcelableUri")
            // use Glide for robust URI handling
            try {
                Glide.with(this)
                    .load(parcelableUri)
                    .centerCrop()
                    .into(storyBackground)
            } catch (e: Exception) {
                Log.w(TAG, "Glide failed to load parcelableUri, falling back", e)
                storyBackground?.setImageURI(parcelableUri)
            }
        } else {
            // 2) Fallback: string extra containing URI
            val storyUriString = intent?.getStringExtra("story_uri")
            if (!storyUriString.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(storyUriString)
                    Log.d(TAG, "Applying StoryBackground from URI string: $storyUriString")
                    Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .into(storyBackground)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse/set story_uri string: $storyUriString", e)
                }
            } else {
                // 3) Fallback: base64 payload from Firestore (if provided)
                val storyBase64 = intent?.getStringExtra("story_base64")
                if (!storyBase64.isNullOrEmpty()) {
                    try {
                        val imageBytes = android.util.Base64.decode(storyBase64, android.util.Base64.DEFAULT)
                        Log.d(TAG, "Applying StoryBackground from story_base64 (decoded ${imageBytes.size} bytes)")
                        // use Glide to load decoded bytes (handles scaling/memory)
                        Glide.with(this)
                            .asBitmap()
                            .load(imageBytes)
                            .centerCrop()
                            .into(storyBackground)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to decode/set story_base64", e)
                        // continue to drawable fallback below
                    }
                } else {
                    // 4) Final fallback: drawable id
                    val passedDrawable = intent?.getIntExtra("story_drawable", -1) ?: -1
                    if (passedDrawable != -1) {
                        Log.d(TAG, "Applying StoryBackground from drawable id=$passedDrawable")
                        storyBackground?.setImageResource(passedDrawable)
                    } else {
                        Log.d(TAG, "No story_uri, story_base64 or story_drawable provided; leaving default background")
                    }
                }
            }
        }

        // Close button: return to MainActivity5
        val closeBtn = findViewById<TextView>(R.id.Close)
        closeBtn?.setOnClickListener {
            Log.d(TAG, "Close pressed - navigating to MainActivity5")
            val intent = Intent(this, MainActivity5::class.java)
            // clear top to avoid stacking activities if needed
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }
}