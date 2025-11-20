package com.mudassarkhalid.i221072

data class Message(
    val id: String = "",
    val senderUid: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val type: String = "text", // "text" | "image"
    val timestamp: Long = 0L,
    val edited: Boolean = false,
    val deleted: Boolean = false
)

