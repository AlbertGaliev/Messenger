package com.kotlin.messenger2018

import android.content.Intent
import android.opengl.Visibility
import android.os.Build.VERSION_CODES.P
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_messages.*
import kotlinx.android.synthetic.main.last_message_messages.view.*

class MessagesActivity : AppCompatActivity() {

    companion object {
        var currentUser: User? = null
    }

    private val adapter = GroupAdapter<ViewHolder>()
    private val lastMessagesHashMap = HashMap<String, Message>()
    private var currentUserID: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        supportActionBar?.title = resources.getString(R.string.messages)
        if (verifyIsUserLoggedIn()) {
            getCurrentUserInfo()
            last_messages_recyclerView_messages.adapter = adapter
            listenToLastMessages()
        }
    }

    private fun getCurrentUserInfo(){
        val ref = FirebaseFirestore.getInstance().collection("users")
        ref.whereEqualTo("userID", FirebaseAuth.getInstance().uid).get()
            .addOnSuccessListener {
                for (document in it.documents) {
                    currentUser = document.toObject(User::class.java)!!
                }
            }
    }

    private fun verifyIsUserLoggedIn(): Boolean{
        currentUserID = FirebaseAuth.getInstance().uid
        if (currentUserID == null){
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return false
        }
        else{
            return true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.messages_navigation_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.users_list_messages -> {
                val intent = Intent(this, UsersActivity::class.java)
                startActivity(intent)
            }
            R.id.setting_messages -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.log_out_messages -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            R.id.create_group_chat -> {
                val intent = Intent(this, CreateGroupActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun listenToLastMessages(){
            FirebaseFirestore.getInstance()
                .collection("last-messages").document(currentUserID!!)
                .collection("last-messages")
                .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                    if (e != null) {
                        Log.w("LastMessages", "listen:error", e)
                        return@EventListener
                    }
                    for (dc in snapshots!!.documentChanges) {
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                Log.d("LastMessages", dc.document.data.toString())
                                val message = dc.document.toObject(Message::class.java)
                                if (message.fromID == currentUserID){
                                    adapter.add(LastMessageItem(message))
                                    lastMessagesHashMap[message.toID] = message
                                }
                                else if(message.toID == currentUserID){
                                    adapter.add(LastMessageItem(message))
                                    lastMessagesHashMap[message.fromID] = message
                                }
                                refleshMessagesRecyclerView()
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Log.d("LastMessages", dc.document.data.toString())
                                val message = dc.document.toObject(Message::class.java)
                                if (message.fromID == currentUserID){
                                    adapter.add(LastMessageItem(message))
                                    lastMessagesHashMap[message.toID] = message
                                }
                                else if(message.toID == currentUserID){
                                    adapter.add(LastMessageItem(message))
                                    lastMessagesHashMap[message.fromID] = message
                                }
                                refleshMessagesRecyclerView()
                            }
                            DocumentChange.Type.REMOVED -> {}
                        }
                    }
                })
            adapter.setOnItemClickListener {item, view ->
                val lastMessageItem = item as LastMessageItem
                val intent = Intent(view.context, ChatActivity::class.java)
                var  companionID: String
                if (lastMessageItem.message.fromID == FirebaseAuth.getInstance().uid){
                    companionID = lastMessageItem.message.toID
                }
                else{
                    companionID = lastMessageItem.message.fromID
                }
                FirebaseFirestore.getInstance()
                    .collection("users").document(companionID)
                    .get()
                    .addOnSuccessListener {
                        val companion = it.toObject(User::class.java)!!
                        intent.putExtra("Key", companion)
                        startActivity(intent)
                    }
            }
    }

    private fun refleshMessagesRecyclerView(){
        adapter.clear()
        lastMessagesHashMap.values.forEach{
            adapter.add(LastMessageItem(it))
        }
    }


    class LastMessageItem(val message: Message): Item<ViewHolder>(){
        override fun bind(viewHolder: ViewHolder, position: Int) {
            val companionID: String
            if (message.fromID == FirebaseAuth.getInstance().uid){
                companionID = message.toID
            }
            else{
                companionID = message.fromID
            }
            FirebaseFirestore.getInstance()
                .collection("users").document(companionID)
                .get()
                .addOnSuccessListener {
                        val companion = it.toObject(User::class.java)!!
                        viewHolder.itemView.last_message_userName_textView.text = companion.userName
                        Picasso.get().load(companion.userImage).into(viewHolder.itemView.last_message_userImage_imageView)
                        when (message.messageType){
                            "TEXT" -> {
                                if (message.fromID == companionID) {
                                    viewHolder.itemView.last_message_message_textView.text = message.text
                                }
                                else {
                                    viewHolder.itemView.last_message_message_textView.text = "Вы: " + message.text
                                }
                            }
                            "IMAGE" -> {
                                if (message.fromID == companionID) {
                                    viewHolder.itemView.last_message_message_textView.text = "Фотография"
                                }
                                else {
                                    viewHolder.itemView.last_message_message_textView.text = "Вы: Фотография"
                                }
                            }
                        }
                        viewHolder.itemView.last_message_userImage_imageView.visibility = View.VISIBLE
                        viewHolder.itemView.last_message_userName_textView.visibility = View.VISIBLE
                        viewHolder.itemView.last_message_message_textView.visibility = View.VISIBLE
                }

        }

        override fun getLayout(): Int {
            return  R.layout.last_message_messages
        }

    }
}

