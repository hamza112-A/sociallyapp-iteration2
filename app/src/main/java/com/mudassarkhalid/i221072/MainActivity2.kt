package com.mudassarkhalid.i221072

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
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

        // Top-right Login button navigates to MainActivity4
        val loginTop = findViewById<AppCompatButton>(R.id.login_top)
        loginTop.setOnClickListener {
            val intent = Intent(this, MainActivity4::class.java)
            startActivity(intent)
        }
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
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity2", "Attempting registration with email: $email and password: $password")

        // Register user with FirebaseAuth
        val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val firebaseUser = firebaseAuth.currentUser
                    val authUid = firebaseUser?.uid ?: ""

                    // Prepare profile image as Base64 (if selected)
                    var profileImageBase64 = ""
                    selectedImageUri?.let { uri ->
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        val imageBytes = outputStream.toByteArray()
                        profileImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                    }

                    // Save user info to Firestore
                    val db = Firebase.firestore
                    val userMap = hashMapOf(
                        "username" to username,
                        "firstName" to yourName,
                        "lastName" to yourLastName,
                        "address" to address,
                        "dob" to dob,
                        "email" to email,
                        "password" to password,
                        "authUid" to authUid,
                        "profileImageBase64" to profileImageBase64
                    )
                    Log.d("MainActivity2", "Attempting Firestore write for user: $authUid")
                    db.collection("users").document(authUid)
                        .set(userMap)
                        .addOnSuccessListener {
                            Log.d("MainActivity2", "Firestore write successful for user: $authUid")
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_LONG).show()
                            // Optionally, navigate to login or main screen
                            val intent = Intent(this, MainActivity4::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("MainActivity2", "Firestore write failed for user $authUid: ${e.localizedMessage}")
                            Toast.makeText(this, "Failed to save user to database: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                        .addOnCanceledListener {
                            Log.e("MainActivity2", "Firestore write was canceled for user $authUid")
                            Toast.makeText(this, "Firestore write was canceled.", Toast.LENGTH_LONG).show()
                        }
                } else {
                    val errorMsg = authTask.exception?.localizedMessage ?: "Unknown error"
                    Log.e("MainActivity2", "Registration failed for email $email: $errorMsg")
                    if (errorMsg.contains("email address is already in use", ignoreCase = true) || errorMsg.contains("EMAIL_EXISTS", ignoreCase = true)) {
                        Toast.makeText(this, "This email is already registered. Please use a different email or log in.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Registration failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
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
                    val canvas = android.graphics.Canvas(bmp)
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
            val canvas = android.graphics.Canvas(bmp)
            canvas.drawColor(android.graphics.Color.LTGRAY)
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
