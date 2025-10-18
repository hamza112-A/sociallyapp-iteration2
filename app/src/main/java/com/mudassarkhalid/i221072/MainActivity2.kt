package com.mudassarkhalid.i221072

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Patterns
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.ByteArrayOutputStream
import kotlin.math.min

class MainActivity2 : AppCompatActivity() {

    // Keep the currently selected image URI so we can convert it reliably to Base64 later
    private var selectedImageUri: Uri? = null

    // Activity Result API launcher for picking an image from gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Save the selected URI and set it on the ImageView for preview
            selectedImageUri = it
            val profileImage = findViewById<ImageView>(R.id.profile_icon)
            profileImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure Firebase SDK is initialized
        FirebaseApp.initializeApp(this)

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
            createAccountAndSave()
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

    private fun createAccountAndSave() {
        // Collect form fields
        val username = findViewById<android.widget.EditText>(R.id.username).text.toString().trim()
        val yourName = findViewById<android.widget.EditText>(R.id.yourname).text.toString().trim()
        val yourLastName = findViewById<android.widget.EditText>(R.id.yourlastname).text.toString().trim()
        val address = findViewById<android.widget.EditText>(R.id.address).text.toString().trim()
        val dob = findViewById<android.widget.EditText>(R.id.dob).text.toString().trim()
        val email = findViewById<android.widget.EditText>(R.id.email).text.toString().trim()
        val password = findViewById<android.widget.EditText>(R.id.password).text.toString().trim()

        // Basic validation
        if (username.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (yourName.isEmpty()) {
            Toast.makeText(this, "First name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (yourLastName.isEmpty()) {
            Toast.makeText(this, "Last name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (address.isEmpty()) {
            Toast.makeText(this, "Address is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (dob.isEmpty()) {
            Toast.makeText(this, "Date of birth is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please provide a valid email", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Always produce a Base64 image string: try URI, then drawable fallback, then generate default
        var imageBase64: String? = selectedImageUri?.let { uriToBase64(it) }
        if (imageBase64.isNullOrEmpty()) {
            val profileImageView = findViewById<ImageView>(R.id.profile_icon)
            imageBase64 = drawableToBase64(profileImageView)
        }
        if (imageBase64.isNullOrEmpty()) {
            imageBase64 = generateDefaultBase64()
        }

        if (imageBase64.isNullOrEmpty()) {
            // Last resort - this should not happen, but guard
            Toast.makeText(this, "Failed to prepare profile image", Toast.LENGTH_LONG).show()
            return
        }

        // Optional size check (binary size) to avoid huge documents
        try {
            val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
            if (imageBytes.size > 900_000) {
                Toast.makeText(this, "Selected image is too large. Choose a smaller image.", Toast.LENGTH_LONG).show()
                return
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Failed to process image data", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare data map
        val user = hashMapOf<String, Any?>(
            "username" to username,
            "firstName" to yourName,
            "lastName" to yourLastName,
            "address" to address,
            "dob" to dob,
            "email" to email,
            // DO NOT store plaintext passwords in production; for demo only
            "password" to password,
            "profileImageBase64" to imageBase64
        )

        // Save to Firestore
        val db = Firebase.firestore
        db.collection("users")
            .add(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Account created and saved", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity3::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(input)
            input.close()

            // downscale large images
            bitmap = scaleBitmap(bitmap, 1024)

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBase64(imageView: ImageView): String? {
        val drawable = imageView.drawable ?: return null
        val bitmap: Bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> {
                try {
                    val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.takeIf { it > 0 } ?: 1,
                        drawable.intrinsicHeight.takeIf { it > 0 } ?: 1,
                        Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                } catch (_: Exception) {
                    return null
                }
            }
        }

        val scaled = scaleBitmap(bitmap, 1024)

        return try {
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    private fun generateDefaultBase64(): String? {
        return try {
            val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.LTGRAY)
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos)
            val bytes = baos.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimensionPx: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val max = if (width >= height) width else height
        if (max <= maxDimensionPx) return bitmap
        val ratio = min(maxDimensionPx.toFloat() / width, maxDimensionPx.toFloat() / height)
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
