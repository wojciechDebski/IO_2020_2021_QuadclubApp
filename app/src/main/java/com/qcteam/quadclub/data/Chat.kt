package com.qcteam.quadclub.data

import com.google.firebase.database.IgnoreExtraProperties


data class ConversationFF(
    val databaseId: String? = null,
    val senderUid: String? = null,
    val senderNameSurname: String? = null,
    val senderPhotoUrl: String? = null,
    val senderDeletedConversation: Boolean? = false,
    val lastMessage: String? = null,
    val companyMessage: Boolean? = false
)


@IgnoreExtraProperties
data class SingleMessage(
    var text: String? = null,
    var sentPhotoUrl: String? = null,
    var senderUid: String? = null,
    var timestamp: String? = null,
)



