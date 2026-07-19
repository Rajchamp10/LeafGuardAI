package com.example.leafguardai

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        // Fluid Background
        val rootLayout = findViewById<ConstraintLayout>(R.id.registerRootLayout)
        val animDrawable = rootLayout.background as AnimationDrawable
        animDrawable.setEnterFadeDuration(1000)
        animDrawable.setExitFadeDuration(4000)
        animDrawable.start()

        val etEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)

        findViewById<TextView>(R.id.txtGoToLogin).setOnClickListener { finish() }

        // --- NEW SWIPE REGISTER LOGIC ---
        val btnSwipeRegister = findViewById<com.ncorti.slidetoact.SlideToActView>(R.id.btnSwipeRegister)

        btnSwipeRegister.onSlideCompleteListener = object : com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: com.ncorti.slidetoact.SlideToActView) {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this@RegisterActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    view.setCompleted(completed = false, withAnimation = true) // Updated to fix warning!
                    return
                }

                // Strict Email Validator
                val strictEmailPattern = "^[a-zA-Z0-9]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
                if (!email.matches(strictEmailPattern.toRegex())) {
                    Toast.makeText(this@RegisterActivity, "Invalid email format! No dots or special characters before the @ symbol.", Toast.LENGTH_LONG).show()
                    view.setCompleted(completed = false, withAnimation = true) // Updated
                    return
                }

                if (password.length < 6) {
                    Toast.makeText(this@RegisterActivity, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    view.setCompleted(completed = false, withAnimation = true) // Updated
                    return
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@RegisterActivity) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@RegisterActivity, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@RegisterActivity, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            view.setCompleted(completed = false, withAnimation = true) // Updated
                        }
                    }
            }
        }
    }
}