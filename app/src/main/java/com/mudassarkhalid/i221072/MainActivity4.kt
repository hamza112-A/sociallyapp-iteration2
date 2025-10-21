package com.mudassarkhalid.i221072

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity4 : AppCompatActivity() {
    private val TAG = "MainActivity4"

    // Runtime-safe check for debug build: try to read generated BuildConfig.DEBUG; fall back to false.
    private fun isDebugBuild(): Boolean {
        return try {
            val pkg = this.packageName
            val cls = Class.forName("$pkg.BuildConfig")
            val field = cls.getField("DEBUG")
            field.getBoolean(null)
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to read BuildConfig.DEBUG via reflection: ${t.message}")
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure Firebase is initialized before using Firestore/Auth
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main4)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI references
        val usernameInput = findViewById<EditText>(R.id.username)
        val passwordInput = findViewById<EditText>(R.id.password)
        val button = findViewById<AppCompatButton>(R.id.button)
        val progress = findViewById<ProgressBar>(R.id.login_progress)

        // Back icon behavior (if present)
        val back = findViewById<android.widget.ImageView>(R.id.back_icon)
        back?.setOnClickListener { finish() }

        fun setUiEnabled(enabled: Boolean) {
            usernameInput.isEnabled = enabled
            passwordInput.isEnabled = enabled
            button.isEnabled = enabled
            progress.visibility = if (enabled) View.GONE else View.VISIBLE
        }

        // Login flow: validate non-empty fields and check credentials in Firestore
        button.setOnClickListener {
            val enteredUsername = usernameInput.text.toString().trim()
            val enteredPassword = passwordInput.text.toString().trim()

            if (enteredUsername.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_username), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (enteredPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable UI while checking
            setUiEnabled(false)

            val db = Firebase.firestore

            db.collection("users")
                .whereEqualTo("username", enteredUsername)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        // No user with that username
                        Toast.makeText(this, "Username or password is not correct", Toast.LENGTH_LONG).show()
                        setUiEnabled(true)
                        return@addOnSuccessListener
                    }

                    val doc = querySnapshot.documents[0]
                    val firestoreUserId = doc.id
                    val storedPassword = doc.getString("password") ?: ""
                    val authUid = doc.getString("authUid") ?: ""

                    if (isDebugBuild()) {
                        Log.d(TAG, "Found user doc=$firestoreUserId, storedPwdLen=${storedPassword.length}")
                    }

                    if (storedPassword == enteredPassword) {
                        // Credentials match: proceed
                        val userName = doc.getString("username") ?: ""
                        val userEmail = doc.getString("email") ?: ""
                        val userProfile = doc.getString("profileImageBase64") ?: ""

                        // Sign in with FirebaseAuth using email and password
                        val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        firebaseAuth.signInWithEmailAndPassword(userEmail, enteredPassword)
                            .addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    // Save session
                                    val sessionManager = SessionManager(this)
                                    sessionManager.saveSession(authUid, userName, userEmail, userProfile)

                                    val intent = Intent(this, MainActivity5::class.java)
                                    intent.putExtra("USER_ID", firestoreUserId)
                                    intent.putExtra("AUTH_UID", authUid)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, "FirebaseAuth sign-in failed: " + (authTask.exception?.localizedMessage ?: "Unknown error"), Toast.LENGTH_LONG).show()
                                }
                                setUiEnabled(true)
                            }
                    } else {
                        // Password mismatch
                        Toast.makeText(this, "Username or password is not correct", Toast.LENGTH_LONG).show()
                        setUiEnabled(true)
                    }

                    setUiEnabled(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to query users: ${e.localizedMessage}")
                    Toast.makeText(this, "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    setUiEnabled(true)
                }
        }
    }
}