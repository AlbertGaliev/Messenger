package com.kotlin.messenger2018

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_users.*
import kotlinx.android.synthetic.main.user_item_userslist.view.*

class UsersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        supportActionBar?.title = resources.getString(R.string.usersList)
        getUsers()
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
                adapter.setOnItemClickListener {item, view ->
                    val userItem = item as UserItem
                    val intent = Intent(view.context, ChatActivity::class.java)
                    intent.putExtra("Key", userItem.user)
                    startActivity(intent)
                    finish()
                }
                users_recyclerView_usersList.adapter = adapter
            }
            .addOnFailureListener {
                Log.w("Users Activity", "Error getting documents.")
            }
    }
}


class UserItem(val user: User): Item<ViewHolder>(){
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.username_usersList.text = user.userName
        Picasso.get().load(user.userImage).into(viewHolder.itemView.userImage_userList)
    }

    override fun getLayout(): Int {
        return R.layout.user_item_userslist
    }
}
