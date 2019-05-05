package com.kotlin.messenger2018

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.chat_item_from_chat.view.*
import kotlinx.android.synthetic.main.chat_item_picture_from_chat.view.*
import kotlinx.android.synthetic.main.chat_item_picture_to_chat.view.*
import kotlinx.android.synthetic.main.chat_item_to_chat.view.*
import java.lang.Exception
import java.util.*


class ChatActivity : AppCompatActivity() {

    companion object {
        val TAG = "Chat Activity"
    }
    private var adapter = GroupAdapter<ViewHolder>()
    private var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chat_recyclerView_chat.adapter = adapter
        toUser = intent.getParcelableExtra<User>("Key")
        supportActionBar?.title = toUser?.userName

        /*var mToolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.chat_toolbar)
        setSupportActionBar(mToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle("MyTitle")*/
        send_button_chat.setOnClickListener {
            performSendMessage()
        }

        add_photo_button_chat.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        listenToMessages()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val selectedImage : Uri?
        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImage = data.data
            performSendPhoto(selectedImage)
        }
    }

    private fun performSendPhoto(selectedImage: Uri?){
        if (selectedImage != null){
            val fileName = UUID.randomUUID().toString()
            val ref= FirebaseStorage.getInstance().getReference("/images/$fileName")
            ref.putFile(selectedImage)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        val text = it.toString()
                        val fromID = MessagesActivity.currentUser!!.userID
                        val toID = toUser?.userID
                        val messageID = UUID.randomUUID().toString()
                        val message = Message(messageID, fromID, toID!!, text, System.currentTimeMillis() / 1000, "IMAGE")
                        addMessageToDatabase(message)
                    }

                }
        }

    }

    private fun performSendMessage(){
        val text = editText_chat.text.toString()
        if (text == "")
            return
        val fromID = MessagesActivity.currentUser!!.userID
        val toID = toUser?.userID
        val messageID = UUID.randomUUID().toString()
        val message = Message(messageID, fromID, toID!!, text, System.currentTimeMillis() / 1000)
        editText_chat.text.clear()
        addMessageToDatabase(message)

    }

    private fun listenToMessages(){
        FirebaseFirestore.getInstance()
            .collection("user-messages").document(MessagesActivity.currentUser!!.userID)
            .collection(toUser!!.userID)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.d(TAG, dc.document.data.toString())
                            val message = dc.document.toObject(Message::class.java)
                            if (message.fromID == MessagesActivity.currentUser!!.userID) {
                                adapter.add(ChatFromItem(message, MessagesActivity.currentUser!!))
                            }
                            else {
                                adapter.add(ChatToItem(message, toUser!!))
                            }
                            chat_recyclerView_chat.scrollToPosition(adapter.itemCount - 1)
                        }
                        DocumentChange.Type.MODIFIED -> {}
                        DocumentChange.Type.REMOVED -> {}
                    }
                }
            })
    }

    private fun addMessageToDatabase(message: Message){
        val refFrom = FirebaseFirestore.getInstance()
            .collection("user-messages").document(message.fromID)
            .collection(message.toID).document(message.messageID)
        refFrom.set(message)
        val refTo = FirebaseFirestore.getInstance()
            .collection("user-messages").document(message.toID)
            .collection(message.fromID).document(message.messageID)
        refTo.set(message).
            addOnSuccessListener {
                Log.d(TAG, "Message is sent")
                chat_recyclerView_chat.scrollToPosition(adapter.itemCount - 1)
            }

        FirebaseFirestore.getInstance()
            .collection("last-messages").document(message.fromID)
            .collection("last-messages").document(message.toID)
            .set(message)
        FirebaseFirestore.getInstance()
            .collection("last-messages").document(message.toID)
            .collection("last-messages").document(message.fromID)
            .set(message)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_navigation_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.update_chat -> {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("Key", toUser)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

class ChatFromItem(val message: Message, val user: User): Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        when (message.messageType){
            "TEXT" -> {
                viewHolder.itemView.messageFrom_textView_chat.text = message.text
                Picasso.get().load(user.userImage).into(viewHolder.itemView.userImageFrom_imageView_chat)
            }
            "IMAGE" -> {
                Picasso.get().load(message.text).into(viewHolder.itemView.chat_image_imageView_from_chat)
                Picasso.get().load(user.userImage).into(viewHolder.itemView.userImageFromImage_imageView_chat)
            }
        }
    }

    override fun getLayout(): Int {
        when (message.messageType){
            "TEXT" -> return R.layout.chat_item_from_chat
            "IMAGE" -> return R.layout.chat_item_picture_from_chat
        }
        return 0
    }
}

class ChatToItem(val message: Message, val user: User): Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        when (message.messageType){
            "TEXT" -> {
                viewHolder.itemView.messageTo_textView_chat.text = message.text
                Picasso.get().load(user.userImage).into(viewHolder.itemView.userImageTo_imageView_chat)
            }
            "IMAGE" -> {
                Picasso.get().load(message.text).into(viewHolder.itemView.chat_image_imageView_to_chat)
                Picasso.get().load(user.userImage).into(viewHolder.itemView.userImageToImage_imageView_chat)
            }
        }
    }

    override fun getLayout(): Int {
        when (message.messageType){
            "TEXT" -> return R.layout.chat_item_to_chat
            "IMAGE" -> return R.layout.chat_item_picture_to_chat
        }
        return 0
    }
}

data class Message(val messageID: String = "",
                   val fromID: String = "",
                   val toID: String = "",
                   val text: String = "",
                   val timestamp: Long = 0,
                   val messageType: String = "TEXT")