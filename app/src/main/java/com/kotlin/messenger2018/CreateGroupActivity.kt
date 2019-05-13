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
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_users.*
import kotlinx.android.synthetic.main.create_groupchat_userlist.view.*
import kotlinx.android.synthetic.main.user_item_userslist.view.*
import kotlinx.android.synthetic.main.user_item_userslist.view.userImage_userList
import kotlinx.android.synthetic.main.user_item_userslist.view.username_usersList
import java.nio.file.attribute.GroupPrincipal
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CreateGroupActivity : AppCompatActivity() {

    private var selectedImage: Uri? = null
    private var userID = ArrayList<String>()

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
            uploadImageToStorage()
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

    private fun uploadImageToStorage(){
        if (selectedImage != null){
            val fileName = UUID.randomUUID().toString()
            val ref= FirebaseStorage.getInstance().getReference("/images/$fileName")
            ref.putFile(selectedImage!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        performCreatingGroup(it.toString())
                    }
                }
                .addOnFailureListener {
                    add_users_buttom_create_groupchat.isEnabled = true
                }
        }
        else{
            performCreatingGroup(null)
        }
    }

    private fun performCreatingGroup(groupImage: String?) {
        var chatName = group_chatname_textView_create_groupchat.text.toString()
        if (chatName == ""){
            Toast.makeText(this, "Введите название беседы", Toast.LENGTH_SHORT).show()
            add_users_buttom_create_groupchat.isEnabled = true
            return
        }
        val groupID = UUID.randomUUID().toString()
        if (!userID.contains(MessagesActivity.currentUser!!.userID))
            userID.add(MessagesActivity.currentUser!!.userID)
        val group = Group(groupID, chatName, groupImage, userID)
        Toast.makeText(this, userID.count().toString(), Toast.LENGTH_SHORT).show()
        val ref = FirebaseFirestore.getInstance().collection("groups").document(groupID)
        ref.set(group)
            .addOnSuccessListener {
                Log.d("Register activity", "All data is stored to db for user: $chatName")
                val intent = Intent(this, MessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                add_users_buttom_create_groupchat.isEnabled = true
            }
            .addOnFailureListener {
                Log.d("Register activity", "Error storing data to db for user: $chatName")
                add_users_buttom_create_groupchat.isEnabled = true
            }
        /*for (item in userID){
            val messageID = UUID.randomUUID().toString()
            val message = Message(messageID, item, groupID, "Вы добавлены в группу", System.currentTimeMillis() / 1000, "TEXT")
            FirebaseFirestore.getInstance()
                .collection("last-messages").document(message.toID)
                .collection("last-messages").document(message.fromID)
                .set(message)
        }*/
    }
    private fun getUsers(){
        var adapter = GroupAdapter<ViewHolder>()
        FirebaseFirestore.getInstance().collection("users")
            .get()
            .addOnSuccessListener {
                for (document in it.documents) {
                    val user = document.toObject(User::class.java)!!
                    adapter.add(AddUserItem(user))
                }
                adapter.setOnItemClickListener {item, view ->
                    val userItem = item as AddUserItem
                    userItem.selectItem()
                    if(!userID.contains(userItem.user.userID))
                        userID.add(userItem.user.userID)
                    else
                        userID.remove(userItem.user.userID)
                }
                users_recyclerView_create_groupchat.adapter = adapter
                users_recyclerView_create_groupchat.scrollToPosition(adapter.itemCount - 1)
            }
            .addOnFailureListener {
                Log.w("Users Activity", "Error getting documents.")
            }
    }


}
class AddUserItem(val user: User): Item<ViewHolder>(){
    private var vh: ViewHolder? = null
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.username_usersList_create_groupChat.text = user.userName
        viewHolder.itemView.username_usersList_create_groupChat.hint = user.userID
        Picasso.get().load(user.userImage).into(viewHolder.itemView.userImage_userList_create_groupChat)
        vh = viewHolder
    }

    override fun getLayout(): Int {
        return R.layout.create_groupchat_userlist
    }

    fun selectItem() {
        vh?.itemView?.checkBox?.setChecked(!vh?.itemView?.checkBox?.isChecked()!!)
    }

    fun isItemSelected() :Boolean? {
        return vh?.itemView?.checkBox?.isChecked()!!
    }
}

data class Group(val groupID: String = "",
                 val groupName: String = "",
                 val groupImage: String? = "",
                 val userID: ArrayList<String>? = null)