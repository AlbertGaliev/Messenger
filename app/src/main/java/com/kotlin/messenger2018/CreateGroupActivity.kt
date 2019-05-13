package com.kotlin.messenger2018

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_users.*
import kotlinx.android.synthetic.main.create_groupchat_userlist.view.*
import kotlinx.android.synthetic.main.user_item_userslist.view.*
import kotlinx.android.synthetic.main.user_item_userslist.view.userImage_userList
import kotlinx.android.synthetic.main.user_item_userslist.view.username_usersList
import java.nio.file.attribute.GroupPrincipal
import java.util.*

class CreateGroupActivity : AppCompatActivity() {

    private var selectedImage: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)
        supportActionBar?.title = resources.getString(R.string.createGroupChat)

        getUsers()

        addPhoto_button_create_groupchat.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        add_users_buttom_create_groupchat.setOnClickListener {
            add_users_buttom_create_groupchat.isEnabled = false
            performCreatingGroup()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImage = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
            group_chat_image_imageView_create_groupchat.setImageBitmap(bitmap)
            addPhoto_button_create_groupchat.alpha = 0f
        }
    }
    private fun performCreatingGroup() {
        var chatName = group_chatname_textView_create_groupchat.text.toString()
        if (chatName == ""){
            Toast.makeText(this, "Введите название беседы", Toast.LENGTH_SHORT).show()
            add_users_buttom_create_groupchat.isEnabled = true
            return
        }
        val groupID = UUID.randomUUID().toString()
        //val group = Group(groupID, chatName)
        /*val ref = FirebaseFirestore.getInstance().collection("groups").document(groupID)
        ref.set(group)
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
            }*/
    }
    private fun getUsers(){
        var adapter = GroupAdapter<ViewHolder>()
        FirebaseFirestore.getInstance().collection("users")
            .get()
            .addOnSuccessListener {
                for (document in it.documents) {
                    val user = document.toObject(User::class.java)!!
                    adapter.add(UserItem(user))
                }
                users_recyclerView_create_groupchat.adapter = adapter
            }
            .addOnFailureListener {
                Log.w("CreateGroupChatActivity", "Error getting documents.")
            }
    }

}
class AddUserItem(val user: User): Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.username_usersList.text = user.userName
        Picasso.get().load(user.userImage).into(viewHolder.itemView.userImage_userList)
    }

    override fun getLayout(): Int {
        return R.layout.user_item_userslist
    }
}

data class Group(val groupID: String = "",
                 val groupName: String = "",
                   val userID: List<String>)