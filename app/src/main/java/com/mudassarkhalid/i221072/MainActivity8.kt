package com.mudassarkhalid.i221072

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ValueEventListener
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity8 : AppCompatActivity() {
    private val TAG = "MainActivity8"
    private lateinit var presenceManager: PresenceManager
    private var statusListener: ValueEventListener? = null
    private var observedUid: String? = null
    private lateinit var ivOnlineDot: ImageView

    // UI and adapter
    private lateinit var rvUsers: RecyclerView
    private lateinit var usersAdapter: UsersAdapter
    private var usersListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure FirebaseApp is initialized before Firestore/Database usage
        try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}

        enableEdgeToEdge()
        setContentView(R.layout.activity_main8)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ivOnlineDot = findViewById(R.id.ivOnlineDot)

        // Set the header username to the logged-in user.
        // Preference order: SessionManager.getUserName() -> Firestore users.username -> FirebaseAuth.displayName (non-email) -> "User"
        try {
            val tvUsername = findViewById<TextView>(R.id.tvUsername)
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val session = SessionManager(this)

            // 1) Session-stored username (fast, authoritative if present)
            val sessionName = session.getUserName()?.takeIf { it.isNotBlank() }
            if (sessionName != null) {
                tvUsername.text = sessionName
                Log.d(TAG, "Header username set from session: $sessionName")
            } else {
                // 2) Try to fetch username from Firestore users collection by authUid
                val uid = firebaseUser?.uid
                if (!uid.isNullOrBlank()) {
                    val db = Firebase.firestore
                    db.collection("users")
                        .whereEqualTo("authUid", uid)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snap ->
                            var finalName: String? = null
                            if (!snap.isEmpty) {
                                val fetched = snap.documents[0].getString("username")
                                if (!fetched.isNullOrBlank()) {
                                    finalName = fetched
                                    // Save to session for faster subsequent loads
                                    try {
                                        val existingEmail = session.getUserEmail() ?: firebaseUser.email ?: ""
                                        val existingProfile = session.getUserProfile() ?: ""
                                        session.saveSession(uid, fetched, existingEmail, existingProfile)
                                        Log.d(TAG, "Saved fetched username to session: $fetched")
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Could not save username to session: ${e.localizedMessage}")
                                    }
                                } else {
                                    Log.d(TAG, "Firestore users doc exists but username field is empty")
                                }
                            } else {
                                Log.d(TAG, "No users document found for authUid=$uid")
                            }

                            // 3) If Firestore didn't return a username, try Firebase displayName but only if it doesn't look like an email
                            if (finalName == null) {
                                // firebaseUser is non-null here because uid was non-blank
                                val candidate = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                                if (!candidate.isNullOrBlank() && !candidate.contains("@")) {
                                    finalName = candidate
                                    Log.d(TAG, "Using Firebase displayName as username: $finalName")
                                } else {
                                    Log.d(TAG, "Firebase displayName either blank or looks like an email; skipping as username")
                                }
                            }

                            // 4) Final fallback
                            if (finalName.isNullOrBlank()) {
                                finalName = "User"
                                Log.d(TAG, "Using fallback username: $finalName")
                            }

                            tvUsername.text = finalName
                            Log.d(TAG, "Header username resolved to: $finalName")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to query users collection for username: ${e.localizedMessage}")
                            // firebaseUser is non-null here because uid was non-blank; prefer displayName if not an email
                            val displayCandidate = firebaseUser.displayName?.takeIf { it.isNotBlank() && !it.contains("@") }
                            val fallback = displayCandidate ?: "User"
                            tvUsername.text = fallback
                            Log.d(TAG, "Header username fallback to: $fallback (query failure)")
                        }
                } else {
                    // No uid (not signed in?) - firebaseUser may be null, so keep safe-call here and fallback to non-email displayName
                    val displayCandidate = firebaseUser?.displayName?.takeIf { it.isNotBlank() && !it.contains("@") }
                    val fallback = displayCandidate ?: "User"
                    tvUsername.text = fallback
                    Log.d(TAG, "Header username fallback to: $fallback (no uid)")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set header username: ${e.localizedMessage}")
        }

        // Redirect to MainActivity5 when back_arrow is clicked
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            val intent = Intent(this, MainActivity5::class.java)
            startActivity(intent)
        }

        // RecyclerView + adapter
        rvUsers = findViewById(R.id.rvUsers)
        rvUsers.layoutManager = LinearLayoutManager(this)

        presenceManager = PresenceManager(this)
        // create adapter with click lambda to open MainActivity9 for the clicked user
        usersAdapter = UsersAdapter(presenceManager) { user ->
            val intent = Intent(this, MainActivity9::class.java)
            intent.putExtra("CHAT_FIRESTORE_ID", user.firestoreId)
            intent.putExtra("CHAT_AUTH_UID", user.authUid)
            intent.putExtra("CHAT_USERNAME", user.username)
            startActivity(intent)
        }
        rvUsers.adapter = usersAdapter
    }

    override fun onStart() {
        super.onStart()
        // start presence writing for current user
        presenceManager.start()

        // observe the current user's status and toggle the dot in header
        val session = SessionManager(this)
        observedUid = session.getUserId()
        observedUid?.let { uid ->
            statusListener = presenceManager.observeUserStatus(uid) { state, _ ->
                runOnUiThread {
                    ivOnlineDot.visibility = if (state == "online") View.VISIBLE else View.GONE
                }
            }
        }

        // Start listening to users collection in Firestore and populate the adapter
        val db = Firebase.firestore
        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // ignore or log in production
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val list = mutableListOf<UserRow>()
                for (doc in snapshot.documents) {
                    val firestoreId = doc.id
                    val authUid = doc.getString("authUid") ?: continue
                    val username = doc.getString("username") ?: authUid
                    val profile = doc.getString("profileImageBase64")
                    list.add(UserRow(firestoreId, authUid, username, profile))
                }
                usersAdapter.setUsers(list)
            }
    }

    override fun onStop() {
        super.onStop()
        // stop presence listener and remove observation
        observedUid?.let { uid ->
            statusListener?.let { presenceManager.removeObserver(uid, it) }
        }
        statusListener = null

        // detach users snapshot listener
        usersListener?.remove()
        usersListener = null

        // clear adapter per-row presence listeners
        usersAdapter.clearAllListeners()

        presenceManager.stop()
    }
}