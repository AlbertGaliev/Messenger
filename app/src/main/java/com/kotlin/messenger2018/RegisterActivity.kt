package com.kotlin.messenger2018

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.kotlin.messenger2018.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*
import kotlin.collections.HashMap


class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        register_button_register.setOnClickListener {
            register_button_register.isEnabled = false
            performRegister()
        }

        addPhoto_button_register.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        alreadyHaveAccount_textView_register.setOnClickListener {
            var intent = Intent(this,  LogInActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImage = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
            circle_imageview_register.setImageBitmap(bitmap)
            addPhoto_button_register.alpha = 0f
        }
    }

    private var selectedImage: Uri? = null
    private var userName: String = ""

    private fun performRegister(){
        userName = username_editText_register.text.toString()
        val email = email_editText_register.text.toString()
        val password = password_editText_register.text.toString()
        if (userName == ""){
            Toast.makeText(this, R.string.enterYourUserName, Toast.LENGTH_SHORT).show()
            register_button_register.isEnabled = true
            return
        }
        else if (email == ""){
            Toast.makeText(this, R.string.enterYourEmail, Toast.LENGTH_SHORT).show()
            register_button_register.isEnabled = true
            return
        }
        else if (password == ""){
            Toast.makeText(this, R.string.enterYourPassword, Toast.LENGTH_SHORT).show()
            register_button_register.isEnabled = true
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("Register activity", "User with email $email was created")
                }
                else {
                    Log.d("Register activity", "User with email $email was not created")
                    Toast.makeText(this, "Registration failed:1", Toast.LENGTH_SHORT).show()
                    register_button_register.isEnabled = true
                    return@addOnCompleteListener
                }
                uploadImageToStorage()
            }
    }

    private fun uploadImageToStorage(){
        if (selectedImage != null){
            val fileName = UUID.randomUUID().toString()
            val ref= FirebaseStorage.getInstance().getReference("/images/$fileName")
            ref.putFile(selectedImage!!)
                .addOnSuccessListener {
                    Log.d("Register activity", "Picture is stored to storage for user: $userName")
                    ref.downloadUrl.addOnSuccessListener {
                        addUserToDatabase(it.toString())
                    }
                }
                .addOnFailureListener {
                    Log.d("Register activity", "Error storing picture to storage for user: $userName")
                    register_button_register.isEnabled = true
                }
        }
        else{
            addUserToDatabase(null)
        }
    }

    private fun addUserToDatabase(userImage: String?){
        val userID = FirebaseAuth.getInstance().uid ?:""
        val user = User(userID, userName, userImage)

        val ref = FirebaseFirestore.getInstance().collection("users").document(userID)
        ref.set(user)
            .addOnSuccessListener {
                Log.d("Register activity", "All data is stored to db for user: $userName")
                val intent = Intent(this, MessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                register_button_register.isEnabled = true
            }
            .addOnFailureListener {
                Log.d("Register activity", "Error storing data to db for user: $userName")
                register_button_register.isEnabled = true
            }
    }
}
@Parcelize
data class User(val userID: String = "",
                  val userName: String = "",
                  val userImage: String? = null):Parcelable