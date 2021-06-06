package com.qcteam.quadclub.data.repository

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.qcteam.quadclub.data.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirebaseRepositoryForCompany : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val urls = "https://quadclub-mobileapp-default-rtdb.europe-west1.firebasedatabase.app/"
    private val dbRTF = FirebaseDatabase.getInstance(urls)


    private val errorMessage: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>(null)
    }

    fun getErrorMessageData(): MutableLiveData<Exception> {
        return errorMessage
    }

    private val loggedCompanyInfo: MutableLiveData<CompanyInfo> by lazy {
        MutableLiveData<CompanyInfo>(null)
    }

    fun getLoggedCompanyInfoData(): MutableLiveData<CompanyInfo> {
        return loggedCompanyInfo
    }

    private val companyOrdersList: MutableLiveData<List<ServiceOrder>> by lazy {
        MutableLiveData<List<ServiceOrder>>(null)
    }

    fun getCompanyOrdersListData(): MutableLiveData<List<ServiceOrder>> {
        return companyOrdersList
    }

    private val notificationsList: MutableLiveData<List<Notification>> by lazy {
        MutableLiveData<List<Notification>>(null)
    }

    fun getNotificationsListData(): MutableLiveData<List<Notification>> {
        return notificationsList
    }

    fun resetTaskListeners() {
        errorMessage.postValue(null)
    }

    //functions implementation ------------------------------------------

    fun getLoggedCompanyInfo() {
        errorMessage.postValue(null)
        loggedCompanyInfo.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val docRef =
                db.collection("serviceCompanies").document(auth.currentUser!!.uid)


            docRef.addSnapshotListener { document, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                if (document != null) {
                    try {
                        loggedCompanyInfo.postValue(document.toObject(CompanyInfo::class.java))
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }
                }
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    private fun pushNotification(notificationMsg: String) {
        errorMessage.postValue(null)

        if (auth.currentUser != null && notificationMsg.isNotEmpty()) {

            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val formatted = current.format(formatter)

            val notificationKey =
                dbRTF.reference.child("notifications/${auth.currentUser!!.uid}").push()

            notificationKey.setValue(
                Notification(
                    notificationKey.key,
                    notificationMsg,
                    formatted,
                    false
                )
            ).addOnFailureListener { error ->
                errorMessage.postValue(error)
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun deleteNotification(notificationKey: String) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            dbRTF.reference.child("notifications/${auth.currentUser!!.uid}/$notificationKey")
                .removeValue()
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun displayNotification(notificationKey: String) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            dbRTF.reference.child("notifications/${auth.currentUser!!.uid}/$notificationKey")
                .updateChildren(
                    mapOf(
                        "displayed" to true
                    )
                ).addOnFailureListener { error ->
                errorMessage.postValue(error)
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun getNotificationsList() {
        errorMessage.postValue(null)
        notificationsList.postValue(null)

        if (auth.currentUser != null) {
            val notificationsReference =
                dbRTF.reference.child("notifications/${auth.currentUser!!.uid}")

            notificationsReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val list = mutableListOf<Notification>()
                    for (notificationSnapshot in dataSnapshot.children) {
                        try {
                            val decodedNotification =
                                notificationSnapshot.getValue(Notification::class.java)

                            if (decodedNotification != null) {
                                list.add(decodedNotification)
                            }
                        } catch (error: Exception) {
                            errorMessage.postValue(error)
                        }

                    }
                    notificationsList.postValue(list)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    errorMessage.postValue(databaseError.toException())
                }
            })
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun getCompanyOrders() {
        errorMessage.postValue(null)
        companyOrdersList.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val orderRef =
                db.collection("orders").whereEqualTo("companyUid", auth.currentUser!!.uid)


            orderRef.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                val list = mutableListOf<ServiceOrder>()
                for (document in snapshots!!) {
                    try {
                        val order = document.toObject(ServiceOrder::class.java)
                        list.add(order)
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }

                }

                for (dc in snapshots.documentChanges) {
                    if (dc.type == DocumentChange.Type.MODIFIED) {
                        try {
                            val order = dc.document.toObject(ServiceOrder::class.java)
                            if (order.status?.get(order.status!!.lastIndex)?.orderStatus == "Klient zaakceptował zaproponowaną datę.") {
                                pushNotification(
                                    "Zamówienie nr: ${order.orderNumber} na: ${
                                        order.status?.get(
                                            order.status!!.lastIndex
                                        )?.orderStatus
                                    }"
                                )
                            }
                        } catch (error: Exception) {
                            errorMessage.postValue(error)
                        }
                    }
                }

                companyOrdersList.postValue(list)
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun updateOrderStatus(selectedOrder: ServiceOrder, statusText: String) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val status = mutableListOf<ServiceOrderStatus>()
            selectedOrder.status?.forEach {
                status.add(it)
            }
            status.add(ServiceOrderStatus(statusText))
            val update: Map<String, Any> = if (statusText == "Serwis zakończony") {
                mapOf(
                    "orderInProgress" to false,
                    "orderDone" to true,
                    "status" to status
                )
            } else {
                mapOf(
                    "status" to status
                )
            }

            val orderRef = db.collection("orders")
                .document(selectedOrder.companyTaxIdentityNumber + "_" + selectedOrder.customerUid + "_no" + selectedOrder.orderNumber)

            orderRef.update(update)
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun saveNoteToOrder(selectedOrder: ServiceOrder, noteText: String) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()

            val saved = if (selectedOrder.serviceInformation.isNotEmpty()) {
                "${selectedOrder.serviceInformation}\n-$noteText"
            } else {
                "-$noteText"
            }

            val update = mapOf(
                "serviceInformation" to saved
            )

            val orderRef = db.collection("orders")
                .document(selectedOrder.companyTaxIdentityNumber + "_" + selectedOrder.customerUid + "_no" + selectedOrder.orderNumber)

            orderRef.update(update)
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

}