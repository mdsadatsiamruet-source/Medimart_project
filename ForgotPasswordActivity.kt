package com.example.medimart2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        db = DatabaseHelper(this)

        val fullNameInput = findViewById<EditText>(R.id.forgotFullName)
        val usernameInput = findViewById<EditText>(R.id.forgotUsername)
        val phoneInput = findViewById<EditText>(R.id.forgotPhone)
        val newPasswordInput = findViewById<EditText>(R.id.newPassword)
        val btnReset = findViewById<Button>(R.id.btnResetPassword)

        btnReset.setOnClickListener {
            val name = fullNameInput.text.toString().trim()
            val user = usernameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val newPass = newPasswordInput.text.toString().trim()

            if (name.isNotEmpty() && user.isNotEmpty() && phone.isNotEmpty() && newPass.isNotEmpty()) {
                val success = db.updatePassword(user, name, phone, newPass)
                if (success) {
                    Toast.makeText(this, "Password reset successful! You can now login.", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Invalid Details. User not found or details mismatch.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}