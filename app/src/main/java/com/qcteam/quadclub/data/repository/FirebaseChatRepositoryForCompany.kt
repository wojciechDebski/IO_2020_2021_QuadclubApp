package com.qcteam.quadclub.data.repository

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.qcteam.quadclub.data.*
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class FirebaseChatRepositoryForCompany : ViewModel() {
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

    private val companyConversationsList: MutableLiveData<List<ConversationFF>> by lazy {
        MutableLiveData<List<ConversationFF>>(null)
    }

    fun getCompanyConversationsListData(): MutableLiveData<List<ConversationFF>> {
        return companyConversationsList
    }

    private val taskStatusListener: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(null)
    }

    fun getTaskStatusListenerData(): MutableLiveData<Boolean> {
        return taskStatusListener
    }

    fun resetTaskListeners() {
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)
    }

    //functions implementations

    fun getConversationsList() {
        errorMessage.postValue(null)
        if (auth.currentUser != null) {
            val firestoreRef = dbFF.collection("serviceCompanies").document(auth.currentUser!!.uid)
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
                    companyConversationsList.postValue(convList)
                }
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

    fun sendMessage(msg: String, conversation: ConversationFF) {
        errorMessage.postValue(null)
        if (auth.currentUser != null) {
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
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

    fun sendPhotoMessage(bitmap: Bitmap, conversation: ConversationFF) {
        errorMessage.postValue(null)
        if (auth.currentUser != null) {
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
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

    fun updateLastMessage(conversation: ConversationFF) {
        errorMessage.postValue(null)
        if (auth.currentUser != null) {
            dbRTF.reference.child("conversations/${conversation.databaseId}").limitToLast(1).get()
                .addOnSuccessListener {
                    it.children.forEach { child ->
                        val msg = child.getValue(SingleMessage::class.java)

                        if (msg != null) {
                            val clientFirestoreRef =
                                dbFF.collection("users").document(conversation.senderUid!!)
                                    .collection("conversations")
                                    .document("${conversation.senderUid}_${auth.currentUser!!.uid}")
                            val companyFirestoreRef =
                                dbFF.collection("serviceCompanies").document(auth.currentUser!!.uid)
                                    .collection("conversations")
                                    .document("${auth.currentUser!!.uid}_${conversation.senderUid}")

                            clientFirestoreRef.update(
                                mapOf(
                                    "lastMessage" to msg.text
                                )
                            ).addOnFailureListener { error ->
                                errorMessage.postValue(error)
                                return@addOnFailureListener
                            }

                            companyFirestoreRef.update(
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
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

    fun deleteConversation(conversation: ConversationFF) {
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)
        if (auth.currentUser != null) {
            val storageRef = FirebaseStorage.getInstance()
                .getReference("conversations/${conversation.databaseId}")
            val rtdRef = dbRTF.reference.child("conversations/${conversation.databaseId}")

            val clientFirestoreRef = dbFF.collection("users").document(conversation.senderUid!!)
                .collection("conversations")
                .document("${conversation.senderUid}_${auth.currentUser!!.uid}")
            val companyFirestoreRef =
                dbFF.collection("serviceCompanies").document(auth.currentUser!!.uid)
                    .collection("conversations")
                    .document("${auth.currentUser!!.uid}_${conversation.senderUid}")


            companyFirestoreRef.delete().addOnSuccessListener {
                if (conversation.senderDeletedConversation == true) {
                    storageRef.delete().addOnFailureListener { error ->
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
                    rtdRef.removeValue().addOnFailureListener { error ->
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
                } else {
                    clientFirestoreRef.update(
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
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

}