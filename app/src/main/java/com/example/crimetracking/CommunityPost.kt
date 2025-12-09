package com.example.crimetracking

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class CommunityPost(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val question: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val likes: List<String> = emptyList(), // List of user IDs who liked
    val commentCount: Int = 0
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", Timestamp.now(), emptyList(), 0)
}

data class Comment(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", "", Timestamp.now())
}

