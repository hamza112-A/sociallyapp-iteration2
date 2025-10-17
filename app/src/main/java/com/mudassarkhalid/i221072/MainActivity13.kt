package com.mudassarkhalid.i221072

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity13 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main13)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Redirect to MainActivity15 when btnEditProfile is clicked
        val editProfileBtn = findViewById<android.widget.Button>(R.id.btnEditProfile)
        editProfileBtn.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity15::class.java)
            startActivity(intent)
        }

        // Redirect to MainActivity5 when home_navigation is clicked
        val homeNav = findViewById<android.widget.ImageView>(R.id.home_navigation)
        homeNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity5::class.java)
            startActivity(intent)
        }
        // Redirect to MainActivity6 when search_navigation is clicked
        val searchNav = findViewById<android.widget.ImageView>(R.id.search_navigation)
        searchNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity6::class.java)
            startActivity(intent)
        }
        // Redirect to MainActivity13 when prof_navigation is clicked
        val profNav = findViewById<android.widget.ImageView>(R.id.prof_navigation)
        profNav.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity13::class.java)
            startActivity(intent)
        }
    }
}