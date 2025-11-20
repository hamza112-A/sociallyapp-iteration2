package com.mudassarkhalid.i221072

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserRow(
    val firestoreId: String,
    val authUid: String,
    val username: String,
    val profileImageBase64: String?
)

class UsersAdapter(
    private val presenceManager: PresenceManager,
    private val onUserClick: (UserRow) -> Unit
) : RecyclerView.Adapter<UsersAdapter.VH>() {

    private val data = mutableListOf<UserRow>()
    // track per-holder presence listener so we can remove when recycled
    private val holderListeners = mutableMapOf<RecyclerView.ViewHolder, Pair<String, ValueEventListener>>()

    fun setUsers(list: List<UserRow>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun clearAllListeners() {
        for ((_, pair) in holderListeners) {
            presenceManager.removeObserver(pair.first, pair.second)
        }
        holderListeners.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = data[position]

        holder.tvUsername.text = user.username
        // Load profile image either from base64 (if present) or fallback to placeholder with Glide
        if (!user.profileImageBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(user.profileImageBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.ivAvatar.setImageBitmap(bmp)
            } catch (_: Exception) {
                Glide.with(holder.itemView).load(R.drawable.download).into(holder.ivAvatar)
            }
        } else {
            Glide.with(holder.itemView).load(R.drawable.download).into(holder.ivAvatar)
        }

        // Clear existing row state
        holder.ivOnlineDot.visibility = View.GONE
        holder.tvLastMessage.text = ""

        // Remove any previous listener attached to this holder
        holderListeners[holder]?.let { (prevUid, prevListener) ->
            presenceManager.removeObserver(prevUid, prevListener)
            holderListeners.remove(holder)
        }

        // Attach presence listener for this row's user
        val listener = presenceManager.observeUserStatus(user.authUid) { state, lastChanged ->
            // UI update must be on main thread; ViewHolder may be reused — ensure we set visibility on the current holder
            holder.itemView.post {
                holder.ivOnlineDot.visibility = if (state == "online") View.VISIBLE else View.GONE

                val lastText = when {
                    state == "online" -> "online"
                    lastChanged != null -> {
                        val short = formatRelativeShort(lastChanged)
                        "last seen $short"
                    }
                    else -> "offline"
                }
                holder.tvLastMessage.text = lastText
            }
        }
        holderListeners[holder] = Pair(user.authUid, listener)

        // Wire click to caller
        holder.itemView.setOnClickListener { onUserClick(user) }

        // Optionally set click to open chat/profile — not implemented here
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holderListeners[holder]?.let { (uid, listener) ->
            presenceManager.removeObserver(uid, listener)
            holderListeners.remove(holder)
        }
    }

    override fun getItemCount(): Int = data.size

    // Helper: convert millis timestamp to short relative string (now, Xm, Xh, Xd, or date)
    private fun formatRelativeShort(timestampMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMillis
        if (diff < 60_000L) return "now"
        val minutes = diff / 60_000L
        if (minutes < 60L) return "${minutes}m"
        val hours = minutes / 60L
        if (hours < 24L) return "${hours}h"
        val days = hours / 24L
        if (days < 7L) return "${days}d"
        // older than a week: show short date like "Nov 2"
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        return fmt.format(Date(timestampMillis))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsernameRow)
        val ivOnlineDot: ImageView = itemView.findViewById(R.id.ivOnlineDotRow)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
    }
}
