package com.mudassarkhalid.i221072

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity2 : AppCompatActivity() {

    // Activity Result API launcher for picking an image from gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Directly set the picked image URI on the ImageView (simple, no saving)
            val profileImage = findViewById<ImageView>(R.id.profile_icon)
            profileImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Connect sign-up button
        val signUpButton = findViewById<AppCompatButton>(R.id.button)
        signUpButton.setOnClickListener {
            handleSignUp()
        }

        // Wire up profile image and camera overlay to open the image picker
        val profileImage = findViewById<ImageView>(R.id.profile_icon)
        val cameraOverlay = findViewById<ImageView>(R.id.cmra)

        val pickListener = {
            // Launch the system picker for images
            pickImageLauncher.launch("image/*")
        }

        profileImage.setOnClickListener { pickListener() }
        cameraOverlay.setOnClickListener { pickListener() }
    }

    private fun handleSignUp() {
        // ✅ Show a toast
        Toast.makeText(applicationContext, "Account created!", Toast.LENGTH_SHORT).show()

        // ✅ Move to Sign In screen (MainActivity3)
        val intent = Intent(this, MainActivity3::class.java)
        startActivity(intent)
        finish() // optional: prevent going back to signup
    }
}
