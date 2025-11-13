package com.mudassarkhalid.i221072

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesAdapter(
    private val currentUserUid: String,
    private val onEdit: (Message) -> Unit,
    private val onDelete: (Message) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.VH>() {

    private val data = mutableListOf<Message>()

    fun setMessages(list: List<Message>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = data[position]

        // Reset
        holder.tvMessageText.visibility = View.GONE
        holder.ivMessageImage.visibility = View.GONE
        holder.tvEdited.visibility = View.GONE

        if (msg.deleted) {
            holder.tvMessageText.visibility = View.VISIBLE
            holder.tvMessageText.text = "Message deleted"
            holder.tvMessageText.setBackgroundResource(R.drawable.rectangle)
            holder.tvMessageText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray))
        } else if (msg.type == "image" && !msg.imageUrl.isNullOrEmpty()) {
            holder.ivMessageImage.visibility = View.VISIBLE
            Glide.with(holder.itemView).load(msg.imageUrl).into(holder.ivMessageImage)
        } else {
            holder.tvMessageText.visibility = View.VISIBLE
            holder.tvMessageText.text = msg.text ?: ""
        }

        holder.tvEdited.visibility = if (msg.edited) View.VISIBLE else View.GONE

        // Timestamp
        holder.tvTimestamp.text = formatTime(msg.timestamp)

        // Long click for edit/delete if sender is current user and within 5 minutes
        holder.itemView.setOnLongClickListener {
            val isOwner = msg.senderUid == currentUserUid
            val allowed = (System.currentTimeMillis() - msg.timestamp) <= (5 * 60 * 1000)
            if (isOwner && allowed && !msg.deleted) {
                val options = arrayOf("Edit", "Delete", "Cancel")
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Message actions")
                    .setItems(options) { dialog, which ->
                        when (which) {
                            0 -> onEdit(msg)
                            1 -> onDelete(msg)
                            else -> dialog.dismiss()
                        }
                    }
                    .show()
                true
            } else {
                false
            }
        }
    }

    override fun getItemCount(): Int = data.size

    private fun formatTime(ts: Long): String {
        if (ts <= 0L) return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessageText: TextView = itemView.findViewById(R.id.tvMessageText)
        val ivMessageImage: ImageView = itemView.findViewById(R.id.ivMessageImage)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvEdited: TextView = itemView.findViewById(R.id.tvEdited)
    }
}
