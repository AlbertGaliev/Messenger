package com.kotlin.messenger2018

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.kotlin.messenger2018.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LogInActivity: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        login_button_login.setOnClickListener {
            login_button_login.isEnabled = false
            performLogIn()
        }

        returnToRegister_textView_login.setOnClickListener {
            finish()
        }
    }

    private fun performLogIn(){
        val email = email_editText_login.text.toString()
        val password = password_editText_login.text.toString()
        if (email == ""){
            Toast.makeText(this, R.string.enterYourEmail, Toast.LENGTH_SHORT).show()
            login_button_login.isEnabled = true
            return
        }
        else if (password == ""){
            Toast.makeText(this, R.string.enterYourPassword, Toast.LENGTH_SHORT).show()
            login_button_login.isEnabled = true
            return
        }
        Log.d("LogIn", "E-mail: $email")
        Log.d("LogIn", "Password: $password")

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{
                if (it.isSuccessful){
                    Log.d("LogIn Activity", "User with email $email entered")
                    val intent = Intent(this, MessagesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                else {
                    Log.d("LogIn Activity", "User with email $email not entered")
                    Toast.makeText(this, R.string.wrongLoginOrPassword, Toast.LENGTH_SHORT).show()
                }
                login_button_login.isEnabled = true
            }
    }
}