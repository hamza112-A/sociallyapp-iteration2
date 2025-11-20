package com.mudassarkhalid.i221072

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot

// Simple data holder for a post
data class Post(
    val id: String,
    val userId: String,
    val imageBase64: String?,
    val username: String?,
    val userProfile: String?,
    val timestamp: Long = 0L
)

class PostAdapter(private val items: MutableList<Post> = mutableListOf()) :
    RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profile: ImageView = view.findViewById(R.id.item_profile)
        val username: TextView = view.findViewById(R.id.item_username)
        val postImage: ImageView = view.findViewById(R.id.item_post_image)
        val like: ImageView? = view.findViewById(R.id.item_like)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = items[position]

        // username
        holder.username.text = post.username ?: ""

        // load profile image if available (base64), otherwise leave default
        try {
            val prof = post.userProfile
            if (!prof.isNullOrEmpty()) {
                val profBytes = Base64.decode(prof, Base64.DEFAULT)
                Glide.with(holder.profile.context)
                    .asBitmap()
                    .load(profBytes)
                    .circleCrop()
                    .into(holder.profile)
            }
        } catch (_: Exception) {
        }

        // load post image (base64)
        try {
            val img = post.imageBase64
            if (!img.isNullOrEmpty()) {
                val bytes = Base64.decode(img, Base64.DEFAULT)
                Glide.with(holder.postImage.context)
                    .asBitmap()
                    .load(bytes)
                    .centerCrop()
                    .into(holder.postImage)
            }
        } catch (_: Exception) {
        }

        // note: like/comment/share buttons are left with their default behavior for now
    }

    override fun getItemCount(): Int = items.size

    fun setPosts(newPosts: List<Post>) {
        items.clear()
        items.addAll(newPosts)
        notifyDataSetChanged()
    }

    // convenience: build a Post from a Firestore document
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Post {
            val id = doc.id
            val userId = doc.getString("userId") ?: ""
            val imageBase64 = doc.getString("imageBase64")
            val username = doc.getString("username")
            val userProfile = doc.getString("userProfile")
            val ts = doc.getLong("timestamp") ?: 0L
            return Post(id, userId, imageBase64, username, userProfile, ts)
        }
    }
}
