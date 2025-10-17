package com.mudassarkhalid.i221072

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity5 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main5)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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

        // Home button does nothing (remains on Activity5)
        val homeNav = findViewById<android.widget.ImageView>(R.id.home_navigation)
        homeNav.setOnClickListener {
            // Do nothing, stay on Activity5
        }

        // Redirect to MainActivity8 when send icon is clicked
        val sendIcon = findViewById<android.widget.ImageView>(R.id.send)
        sendIcon.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity8::class.java)
            startActivity(intent)
        }
    }
}