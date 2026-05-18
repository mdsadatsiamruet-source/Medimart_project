package com.example.medimart2

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var phone: EditText
    private lateinit var signupBtn: Button
    private lateinit var loginRedirectBtn: Button
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        db = DatabaseHelper(this)

        name = findViewById(R.id.name)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirmPassword)
        phone = findViewById(R.id.phone)
        signupBtn = findViewById(R.id.signupBtn)
        loginRedirectBtn = findViewById(R.id.loginRedirectBtn)

        signupBtn.setOnClickListener {
            val fullName = name.text.toString()
            val user = username.text.toString()
            val pass = password.text.toString()
            val confPass = confirmPassword.text.toString()
            val phn = phone.text.toString()

            if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty() || confPass.isEmpty() || phn.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            } else if (pass.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            } else if (pass != confPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else if (!phn.startsWith("+880")) {
                Toast.makeText(this, "Phone number must start with +880", Toast.LENGTH_SHORT).show()
            } else if (phn.length != 14) {
                Toast.makeText(this, "Phone number must be 14 characters long (e.g. +8801700000000)", Toast.LENGTH_SHORT).show()
            } else if (db.checkUsernameExists(user)) {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
            } else {
                val success = db.insertUser(fullName, user, pass, phn)
                if (success) {
                    Toast.makeText(this, "Account Created", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loginRedirectBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}