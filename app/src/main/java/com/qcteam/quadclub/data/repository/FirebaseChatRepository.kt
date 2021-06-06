package com.qcteam.quadclub.data.repository

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.qcteam.quadclub.data.*
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class FirebaseChatRepository : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val urls = "https://quadclub-mobileapp-default-rtdb.europe-west1.firebasedatabase.app/"
    private val dbRTF = FirebaseDatabase.getInstance(urls)
    private val dbFF = FirebaseFirestore.getInstance()

    private val errorMessage: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>(null)
    }

    fun getErrorMessageData(): MutableLiveData<Exception> {
        return errorMessage
    }

    private val userConversationsList: MutableLiveData<List<ConversationFF>> by lazy {
        MutableLiveData<List<ConversationFF>>(null)
    }

    fun getUserConversationsListData(): MutableLiveData<List<ConversationFF>> {
        return userConversationsList
    }

    private val usersForConversationList: MutableLiveData<List<UserInfoForConversations>> by lazy {
        MutableLiveData<List<UserInfoForConversations>>(null)
    }

    fun getUsersForConversationListData(): MutableLiveData<List<UserInfoForConversations>> {
        return usersForConversationList
    }

    private val newConversationStatus: MutableLiveData<String> by lazy {
        MutableLiveData<String>(null)
    }

    fun getNewConversationStatusData(): MutableLiveData<String> {
        return newConversationStatus
    }

    private val taskStatusListener: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(null)
    }

    fun getTaskStatusListenerData(): MutableLiveData<Boolean> {
        return taskStatusListener
    }

    fun resetTaskListeners() {
        errorMessage.postValue(null)
        newConversationStatus.postValue(null)
        taskStatusListener.postValue(null)
    }

    //functions implementations

    fun getConversationsList(){
        errorMessage.postValue(null)
        if (auth.currentUser != null) {
            val firestoreRef = dbFF.collection("users").document(auth.currentUser!!.uid)
                .collection("conversations")

            firestoreRef.addSnapshotListener { list, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }


                if (list != null) {
                    val convList = mutableListOf<ConversationFF>()
                    for (item in list) {
                        try {
                            val conversation = item.toObject(ConversationFF::class.java)
                            convList.add(conversation)
                        } catch (error: Exception) {
                            errorMessage.postValue(error)
                        }
                    }
                    userConversationsList.postValue(convList)
                }
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun getUsersForConversationList() {
        errorMessage.postValue(null)
        usersForConversationList.postValue(null)
        if (auth.currentUser != null) {
            val usersRef = dbFF.collection("users")

            usersRef.addSnapshotListener { documents, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                val list = mutableListOf<UserInfoForConversations>()
                for (document in documents!!) {
                    try {
                        val user = document.toObject(UserInfo::class.java)
                        if (!user.isService && document.id != auth.currentUser!!.uid) {
                            val nameSurname = "${user.firstName} ${user.lastName}"
                            val userForConversation = UserInfoForConversations(
                                user.userProfilePhotoUrl,
                                nameSurname,
                                document.id
                            )
                            list.add(userForConversation)
                        }
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }
                }

                usersForConversationList.postValue(list)
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun startNewConversation(partner: UserInfoForConversations, user: UserInfo) {
        errorMessage.postValue(null)
        newConversationStatus.postValue(null)
        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val myFirestoreRef = db.collection("users").document(auth.currentUser!!.uid)
                .collection("conversations")
                .document("${auth.currentUser!!.uid}_${partner.userUid}")
            val partnerFirestoreRef =
                db.collection("users").document(partner.userUid!!).collection("conversations")
                    .document("${partner.userUid}_${auth.currentUser!!.uid}")


            myFirestoreRef.set(
                ConversationFF(
                    "${auth.currentUser!!.uid}_${partner.userUid}",
                    partner.userUid,
                    partner.userNameSurname,
                    partner.userProfilePhotoUrl,
                    null
                )
            ).addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }

            partnerFirestoreRef.set(
                ConversationFF(
                    "${auth.currentUser!!.uid}_${partner.userUid}",
                    auth.currentUser!!.uid,
                    "${user.firstName} ${user.lastName}",
                    user.userProfilePhotoUrl,
                    null
                )
            ).addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }

            dbRTF.reference.child("conversations/${auth.currentUser!!.uid}_${partner.userUid}")
                .push()
                .setValue(
                    SingleMessage(
                        "start_conversation",
                        "",
                        "1053",
                        ""
                    )
                ).addOnSuccessListener {
                    newConversationStatus.postValue("${auth.currentUser!!.uid}_${partner.userUid}")
                }.addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun sendMessage(msg: String, conversation: ConversationFF?, user: UserInfo) {
        errorMessage.postValue(null)
        if (auth.currentUser != null && conversation != null) {

            if (conversation.senderDeletedConversation == true) {
                var companyMessage = false
                val myFirestoreRef = dbFF.collection("users").document(auth.currentUser!!.uid)
                    .collection("conversations")
                    .document("${auth.currentUser!!.uid}_${conversation.senderUid}")

                val partnerFirestoreRef: DocumentReference =
                    if (conversation.companyMessage == true) {
                        companyMessage = true
                        dbFF.collection("serviceCompanies").document(conversation.senderUid!!)
                            .collection("conversations")
                            .document("${conversation.senderUid}_${auth.currentUser!!.uid}")

                    } else {
                        dbFF.collection("users").document(conversation.senderUid!!)
                            .collection("conversations")
                            .document("${conversation.senderUid}_${auth.currentUser!!.uid}")
                    }

                partnerFirestoreRef.set(
                    ConversationFF(
                        conversation.databaseId,
                        auth.currentUser!!.uid,
                        "${user.firstName} ${user.lastName}",
                        user.userProfilePhotoUrl,
                        false,
                        msg,
                        companyMessage
                    )
                ).addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }

                myFirestoreRef.update(
                    mapOf(
                        "senderDeletedConversation" to false
                    )
                ).addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
            }

            val current = LocalDateTime.now()

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val formatted = current.format(formatter)

            dbRTF.reference.child("conversations/${conversation.databaseId}").push()
                .setValue(
                    SingleMessage(
                        msg,
                        "",
                        auth.currentUser!!.uid,
                        formatted
                    )
                ).addOnSuccessListener {
                    updateLastMessage(conversation)
                }.addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun sendPhotoMessage(bitmap: Bitmap, conversation: ConversationFF?, userInfo: UserInfo) {
        errorMessage.postValue(null)
        if (auth.currentUser != null && conversation != null) {

            if (conversation.senderDeletedConversation == true) {
                var companyMessage = false
                val myFirestoreRef = dbFF.collection("users").document(auth.currentUser!!.uid)
                    .collection("conversations")
                    .document("${auth.currentUser!!.uid}_${conversation.senderUid}")

                val partnerFirestoreRef: DocumentReference =
                    if (conversation.companyMessage == true) {
                        companyMessage = true
                        dbFF.collection("serviceCompanies").document(conversation.senderUid!!)
                            .collection("conversations")
                            .document("${conversation.senderUid}_${auth.currentUser!!.uid}")

                    } else {
                        dbFF.collection("users").document(conversation.senderUid!!)
                            .collection("conversations")
                            .document("${conversation.senderUid}_${auth.currentUser!!.uid}")
                    }

                partnerFirestoreRef.set(
                    ConversationFF(
                        conversation.databaseId,
                        auth.currentUser!!.uid,
                        "${userInfo.firstName} ${userInfo.lastName}",
                        userInfo.userProfilePhotoUrl,
                        false,
                        "zdjęcie",
                        companyMessage
                    )
                ).addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }

                myFirestoreRef.update(
                    mapOf(
                        "senderDeletedConversation" to false
                    )
                ).addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
            }

            val filename = UUID.randomUUID().toString()

            val storageRef = FirebaseStorage.getInstance()
                .getReference("conversations/${conversation.databaseId}/$filename")

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data: ByteArray = baos.toByteArray()

            storageRef.putBytes(data).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { photoUrl ->
                    val current = LocalDateTime.now()

                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    val formatted = current.format(formatter)


                    dbRTF.reference.child("conversations/${conversation.databaseId}").push()
                        .setValue(
                            SingleMessage(
                                "zdjęcie",
                                photoUrl.toString(),
                                auth.currentUser!!.uid,
                                formatted
                            )
                        ).addOnSuccessListener {
                            updateLastMessage(conversation)
                        }.addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                }.addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
            }.addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun updateLastMessage(conversation: ConversationFF) {
        errorMessage.postValue(null)
        if (auth.currentUser != null) {
            dbRTF.reference.child("conversations/${conversation.databaseId}").limitToLast(1).get()
                .addOnSuccessListener {
                    it.children.forEach { child ->
                        val msg = child.getValue(SingleMessage::class.java)

                        if (msg != null && msg.senderUid != "1053") {
                            val myFirestoreRef =
                                dbFF.collection("users").document(auth.currentUser!!.uid)
                                    .collection("conversations")
                                    .document("${auth.currentUser!!.uid}_${conversation.senderUid}")
                            val partnerFirestoreRef = if (conversation.companyMessage == true) {
                                dbFF.collection("serviceCompanies")
                                    .document(conversation.senderUid!!).collection("conversations")
                                    .document("${conversation.senderUid}_${auth.currentUser!!.uid}")
                            } else {
                                dbFF.collection("users").document(conversation.senderUid!!)
                                    .collection("conversations")
                                    .document("${conversation.senderUid}_${auth.currentUser!!.uid}")
                            }


                            myFirestoreRef.update(
                                mapOf(
                                    "lastMessage" to msg.text
                                )
                            ).addOnFailureListener { error ->
                                errorMessage.postValue(error)
                                return@addOnFailureListener
                            }

                            partnerFirestoreRef.update(
                                mapOf(
                                    "lastMessage" to msg.text
                                )
                            ).addOnFailureListener { error ->
                                errorMessage.postValue(error)
                                return@addOnFailureListener
                            }
                        }
                    }
                }.addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun deleteConversation(conversationDatabaseId: String, sender: ConversationFF) {
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)

        if (auth.currentUser != null) {
            val storageRef =
                FirebaseStorage.getInstance().getReference("conversations/$conversationDatabaseId")
            val rtdRef = dbRTF.reference.child("conversations/$conversationDatabaseId")

            val myFirestoreRef = dbFF.collection("users").document(auth.currentUser!!.uid)
                .collection("conversations")
                .document("${auth.currentUser!!.uid}_${sender.senderUid}")

            val partnerFirestoreRef: DocumentReference = if (sender.companyMessage == true) {
                dbFF.collection("serviceCompanies").document(sender.senderUid!!)
                    .collection("conversations")
                    .document("${sender.senderUid}_${auth.currentUser!!.uid}")
            } else {
                dbFF.collection("users").document(sender.senderUid!!).collection("conversations")
                    .document("${sender.senderUid}_${auth.currentUser!!.uid}")
            }


            myFirestoreRef.delete().addOnSuccessListener {
                if (sender.senderDeletedConversation == true) {
                    storageRef.delete().addOnFailureListener { error ->
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
                    rtdRef.removeValue().addOnFailureListener { error ->
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
                } else {
                    partnerFirestoreRef.update(
                        mapOf(
                            "senderDeletedConversation" to true
                        )
                    ).addOnFailureListener { error ->
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
                }
                taskStatusListener.postValue(true)
            }.addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

}


