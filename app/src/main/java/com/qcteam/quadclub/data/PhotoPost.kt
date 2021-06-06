package com.qcteam.quadclub.data

import com.google.firebase.Timestamp

data class PhotoPost(
    var authorNameSurname: String? = null,
    var authorPhotoUrl: String? = null,
    var postPhotoUrl: String? = null,
    var postPhotoPath: String? = null,
    var likesCounter: Int = 0,
    var vehicle: Quad? = null,
    var tags: String? = null,
    var authorUid: String? = null,
    var documentId: String? = null,
    var likedBy: List<String>? = null,
    val timestamp: Timestamp = Timestamp.now()
)