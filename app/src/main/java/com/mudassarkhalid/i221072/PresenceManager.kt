package com.mudassarkhalid.i221072

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

/**
 * Simple presence manager that writes /status/{uid} = { state: "online"|"offline", last_changed: ServerValue.TIMESTAMP }
 * and observes other users' status.
 * It uses SessionManager to obtain the current user's id.
 */
class PresenceManager(private val context: Context) {
    private val db: FirebaseDatabase
        get() = FirebaseDatabase.getInstance()
    private val connectedRef: DatabaseReference
        get() = db.getReference(".info/connected")
    private val statusRef: DatabaseReference
        get() = db.getReference("status")

    private var connectedListener: ValueEventListener? = null

    fun start() {
        // Ensure FirebaseApp is initialized (some other activities already call initializeApp, but guard here)
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
        } catch (e: Exception) {
            // ignore - initialization may already have happened or manifest misconfiguration
        }

        val session = SessionManager(context)
        val uid = session.getUserId() ?: return

        // Listen to connection state and set presence accordingly
        connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                val userStatusRef = statusRef.child(uid)
                if (connected) {
                    userStatusRef.onDisconnect().setValue(mapOf(
                        "state" to "offline",
                        "last_changed" to ServerValue.TIMESTAMP
                    ))
                    userStatusRef.setValue(mapOf(
                        "state" to "online",
                        "last_changed" to ServerValue.TIMESTAMP
                    ))
                } else {
                    // Not connected — set offline
                    userStatusRef.setValue(mapOf(
                        "state" to "offline",
                        "last_changed" to ServerValue.TIMESTAMP
                    ))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // ignore or log
            }
        }
        connectedRef.addValueEventListener(connectedListener as ValueEventListener)
    }

    fun stop() {
        connectedListener?.let { connectedRef.removeEventListener(it) }
        connectedListener = null
    }

    fun observeUserStatus(targetUid: String, callback: (state: String?, lastChanged: Long?) -> Unit): ValueEventListener {
        val ref = statusRef.child(targetUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = snapshot.value as? Map<*, *>
                val state = map?.get("state") as? String
                val last = when (val v = map?.get("last_changed")) {
                    is Long -> v
                    is Double -> v.toLong()
                    else -> null
                }
                callback(state, last)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null, null)
            }
        }
        ref.addValueEventListener(listener)
        return listener
    }

    fun removeObserver(targetUid: String, listener: ValueEventListener) {
        statusRef.child(targetUid).removeEventListener(listener)
    }
}
