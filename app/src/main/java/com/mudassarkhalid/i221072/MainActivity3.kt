package com.mudassarkhalid.i221072

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity3 : AppCompatActivity() {
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI references
        val profileImageView = findViewById<ImageView>(R.id.Profile)
        val usernameEdit = findViewById<EditText>(R.id.Username)
        val loginButton = findViewById<AppCompatButton>(R.id.button)

        // Back arrow
        val arrow = findViewById<ImageView>(R.id.arrow_back)
        arrow.setOnClickListener { finish() }

        // Get user ID passed from MainActivity2
        userId = intent?.getStringExtra("USER_ID")
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, "No user id provided", Toast.LENGTH_LONG).show()
            // disable the login button since we don't have a document to proceed with
            loginButton.isEnabled = false
            return
        }

        // Fetch user document from Firestore
        val db = Firebase.firestore
        val docRef = db.collection("users").document(userId!!)

        // Initially disable login until we successfully load user data
        loginButton.isEnabled = false

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Toast.makeText(this, "User not found", Toast.LENGTH_LONG).show()
                    loginButton.isEnabled = false
                    return@addOnSuccessListener
                }

                // Populate username (read-only)
                val username = snapshot.getString("username")
                if (!username.isNullOrEmpty()) {
                    usernameEdit.setText(username)
                }

                // Load profile image Base64 and decode
                val imageBase64 = snapshot.getString("profileImageBase64")
                if (!imageBase64.isNullOrEmpty()) {
                    try {
                        val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        if (bitmap != null) {
                            profileImageView.setImageBitmap(bitmap)
                        } else {
                            // fallback: keep default drawable
                            Toast.makeText(this, "Failed to decode profile image", Toast.LENGTH_SHORT).show()
                        }
                    } catch (ex: Exception) {
                        Toast.makeText(this, "Error decoding image: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // no image stored - keep default image
                }

                // Enable login button now that user data is loaded
                loginButton.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                loginButton.isEnabled = false
            }

        // Login button: navigate to MainActivity5 (user is considered "logged in" for this flow)
        loginButton.setOnClickListener {
            if (userId.isNullOrBlank()) {
                Toast.makeText(this, "User id not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Optionally pass userId forward
            val intent = Intent(this, MainActivity5::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }
}