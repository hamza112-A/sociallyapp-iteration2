package com.mudassarkhalid.i221072

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity2 : AppCompatActivity() {

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
