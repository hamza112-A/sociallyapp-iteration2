package com.mudassarkhalid.i221072

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.UUID

class MainActivity9 : AppCompatActivity() {
    private val TAG = "MainActivity9"

    // Messaging
    private lateinit var rvMessages: RecyclerView
    private lateinit var messagesAdapter: MessagesAdapter
    private var messagesListener: ListenerRegistration? = null

    private var chatFirestoreId: String? = null
    private var chatAuthUid: String? = null
    private var chatUsername: String = ""
    private var currentUserUid: String? = null
    private var chatId: String = ""

    private val db = Firebase.firestore
    private val storage = Firebase.storage

    // Image picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImageAndSend(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure Firebase initialized
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}

        enableEdgeToEdge()
        setContentView(R.layout.activity_main9)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Read intent extras passed from Activity8
        chatFirestoreId = intent.getStringExtra("CHAT_FIRESTORE_ID")
        chatAuthUid = intent.getStringExtra("CHAT_AUTH_UID")
        chatUsername = intent.getStringExtra("CHAT_USERNAME") ?: ""

        Log.d(TAG, "Chat opened for firestoreId=$chatFirestoreId authUid=$chatAuthUid username=$chatUsername")

        // UI header
        val usernameTv = findViewById<TextView>(R.id.username)
        val profileTv = findViewById<TextView>(R.id.profile)
        usernameTv.text = chatUsername
        val initials = chatUsername.split(" ").filter { it.isNotEmpty() }
            .mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
            .take(2)
            .joinToString("")
        profileTv.text = if (initials.isNotEmpty()) initials else "?"
        findViewById<ImageView>(R.id.backArrow)?.setOnClickListener { finish() }

        // Get canonical current user uid from FirebaseAuth (prefer this over stored session value)
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUid = firebaseAuth.uid
        val session = SessionManager(this)
        val sessionUid = session.getUserId()
        Log.d(TAG, "Session uid=$sessionUid firebaseUid=$firebaseUid")
        currentUserUid = firebaseUid ?: sessionUid
        if (currentUserUid == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Require that we have the other user's auth UID (chatAuthUid). If missing, we cannot compute canonical chatId.
        if (chatAuthUid.isNullOrEmpty()) {
            Toast.makeText(this, "Chat target id missing", Toast.LENGTH_LONG).show()
            Log.e(TAG, "chatAuthUid is null or empty — cannot open chat")
            finish()
            return
        }

        // Compute chatId deterministically from two Firebase Auth UIDs only (canonical ordering)
        val a = currentUserUid!!
        val b = chatAuthUid!!
        chatId = if (a <= b) "${a}_$b" else "${b}_$a"
        Log.d(TAG, "Computed canonical chatId=$chatId (a=$a, b=$b)")

        // Ensure the chat document exists and contains participants (create if missing)
        val chatDocRef = db.collection("chats").document(chatId)
        chatDocRef.get().addOnSuccessListener { snap ->
            if (!snap.exists()) {
                val chatData = mapOf(
                    "participants" to listOf(a, b),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                chatDocRef.set(chatData, SetOptions.merge()).addOnSuccessListener {
                    Log.d(TAG, "Created chat doc $chatId with participants")
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Failed to create chat doc: ${e.localizedMessage}")
                }
            } else {
                // Validate participants field exists; if not, set it
                val parts = snap.get("participants")
                if (parts == null) {
                    chatDocRef.set(mapOf("participants" to listOf(a, b)), SetOptions.merge())
                }
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Chat doc get failed: ${e.localizedMessage}")
        }

        // RecyclerView + adapter
        rvMessages = findViewById(R.id.rvMessages)
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        messagesAdapter = MessagesAdapter(currentUserUid!!, onEdit = { msg -> showEditDialog(msg) }, onDelete = { msg -> performDelete(msg) })
        rvMessages.adapter = messagesAdapter

        // Send button
        findViewById<ImageView>(R.id.sendIcon).setOnClickListener {
            val et = findViewById<EditText>(R.id.messageEditText)
            val text = et.text.toString().trim()
            if (text.isNotEmpty()) {
                sendTextMessage(text)
                et.setText("")
            }
        }

        // Gallery pick
        findViewById<ImageView>(R.id.galleryIcon).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    override fun onStart() {
        super.onStart()
        // Attach messages listener
        val messagesRef = db.collection("chats").document(chatId).collection("messages")
        messagesListener = messagesRef
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Messages listen failed: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                // Detailed logging for debugging two-way messaging
                Log.d(TAG, "snapshot for chatId=$chatId size=${snapshot.size()} metadata=${snapshot.metadata}")
                try {
                    val docsData = snapshot.documents.map { it.data }
                    Log.d(TAG, "snapshot documents data for chatId=$chatId: $docsData")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to enumerate snapshot docs: ${e.localizedMessage}")
                }

                val list = mutableListOf<Message>()
                for (doc in snapshot.documents) {
                    Log.d(TAG, "doc ${doc.id} => ${doc.data}")
                    val id = doc.id
                    val senderUid = doc.getString("senderUid") ?: ""
                    val text = doc.getString("text")
                    val imageUrl = doc.getString("imageUrl")
                    val type = doc.getString("type") ?: if (imageUrl != null) "image" else "text"
                    val edited = doc.getBoolean("edited") ?: false
                    val deleted = doc.getBoolean("deleted") ?: false
                    val tsField = doc.get("timestamp")
                    val timestamp = when (tsField) {
                        is Timestamp -> tsField.toDate().time
                        is Long -> tsField
                        is Double -> tsField.toLong()
                        else -> 0L
                    }
                    list.add(Message(id, senderUid, text, imageUrl, type, timestamp, edited, deleted))
                }
                messagesAdapter.setMessages(list)
                if (list.isNotEmpty()) rvMessages.scrollToPosition(list.size - 1)
            }
    }

    override fun onStop() {
        super.onStop()
        messagesListener?.remove()
        messagesListener = null
    }

    private fun sendTextMessage(text: String) {
        val messagesRef = db.collection("chats").document(chatId).collection("messages")
        // Prefer the live FirebaseAuth uid as the canonical sender id; fall back to cached currentUserUid
        val senderUid = FirebaseAuth.getInstance().uid ?: currentUserUid
        val data = hashMapOf<String, Any?>(
            "senderUid" to senderUid,
            "text" to text,
            "type" to "text",
            "timestamp" to FieldValue.serverTimestamp(),
            "edited" to false,
            "deleted" to false
        )
        Log.d(TAG, "Sending text message chatId=$chatId senderUid=$senderUid payload=$data")
        messagesRef.add(data).addOnSuccessListener {
            Log.d(TAG, "Message sent id=${it.id} chatId=$chatId payload=$data")
        }.addOnFailureListener { e ->
            Log.w(TAG, "Send failed: ${e.localizedMessage}")
            Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageAndSend(uri: Uri) {
        val ref = storage.reference.child("chat_images/$chatId/${UUID.randomUUID()}.jpg")
        val uploadTask = ref.putFile(uri)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
            ref.downloadUrl
        }.addOnSuccessListener { downloadUrl ->
            sendImageMessage(downloadUrl.toString())
        }.addOnFailureListener { e ->
            Log.w(TAG, "Image upload failed: ${e.localizedMessage}")
            Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendImageMessage(imageUrl: String) {
        val messagesRef = db.collection("chats").document(chatId).collection("messages")
        // Prefer live FirebaseAuth uid as sender id
        val senderUid = FirebaseAuth.getInstance().uid ?: currentUserUid
        val data = hashMapOf<String, Any?>(
            "senderUid" to senderUid,
            "imageUrl" to imageUrl,
            "type" to "image",
            "timestamp" to FieldValue.serverTimestamp(),
            "edited" to false,
            "deleted" to false
        )
        Log.d(TAG, "Sending image message chatId=$chatId senderUid=$senderUid payload=$data")
        messagesRef.add(data).addOnSuccessListener {
            Log.d(TAG, "Image message sent id=${it.id} chatId=$chatId payload=$data")
        }.addOnFailureListener { e ->
            Log.w(TAG, "Send image failed: ${e.localizedMessage}")
            Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(msg: Message) {
        // Ensure ownership and 5-minute window
        if (msg.senderUid != currentUserUid) return
        val now = System.currentTimeMillis()
        if (now - msg.timestamp > 5 * 60 * 1000) {
            Toast.makeText(this, "Edit window expired", Toast.LENGTH_SHORT).show()
            return
        }

        // Only text messages are editable; images can be deleted only
        if (msg.type != "text") {
            // Prompt delete confirmation
            AlertDialog.Builder(this)
                .setTitle("Delete message")
                .setMessage("This is a media message. Do you want to delete it?")
                .setPositiveButton("Delete") { dialog, _ ->
                    performDelete(msg)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit message")

        val input = EditText(this)
        input.setText(msg.text ?: "")
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newText = input.text.toString().trim()
            if (newText.isNotEmpty()) {
                performEdit(msg, newText)
            }
            dialog.dismiss()
        }
        builder.setNeutralButton("Delete") { dialog, _ ->
            performDelete(msg)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun performEdit(msg: Message, newText: String) {
        val docRef = db.collection("chats").document(chatId).collection("messages").document(msg.id)
        val updates = hashMapOf<String, Any>(
            "text" to newText,
            "edited" to true
        )
        docRef.update(updates as Map<String, Any>).addOnSuccessListener {
            Log.d(TAG, "Message edited: ${msg.id}")
        }.addOnFailureListener { e ->
            Log.w(TAG, "Edit failed: ${e.localizedMessage}")
            Toast.makeText(this, "Edit failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performDelete(msg: Message) {
        val docRef = db.collection("chats").document(chatId).collection("messages").document(msg.id)
        val updates = hashMapOf<String, Any>(
            "deleted" to true,
            // remove text/image fields using FieldValue.delete() so types are non-null Any
            "text" to FieldValue.delete(),
            "imageUrl" to FieldValue.delete()
        )
        docRef.update(updates as Map<String, Any>).addOnSuccessListener {
            Log.d(TAG, "Message deleted: ${msg.id}")
        }.addOnFailureListener { e ->
            Log.w(TAG, "Delete failed: ${e.localizedMessage}")
            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
        }
    }
}