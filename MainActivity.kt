package com.example.medimart2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirect to LoginActivity immediately
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
