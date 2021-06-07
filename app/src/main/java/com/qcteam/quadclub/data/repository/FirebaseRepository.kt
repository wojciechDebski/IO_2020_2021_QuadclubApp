package com.qcteam.quadclub.data.repository

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.qcteam.quadclub.data.*
import com.qcteam.quadclub.data.enums.SearchParam
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class FirebaseRepository : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val urls = "https://quadclub-mobileapp-default-rtdb.europe-west1.firebasedatabase.app/"
    private val dbRTF = FirebaseDatabase.getInstance(urls)
    private val dbFF = FirebaseFirestore.getInstance()

    //initialization live data variables --------------------------

    private val authorizedUser: MutableLiveData<FirebaseUser> by lazy {
        MutableLiveData<FirebaseUser>(null)
    }

    fun getAuthorizedUserData(): MutableLiveData<FirebaseUser> {
        return authorizedUser
    }

    private val errorMessage: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>(null)
    }

    fun getErrorMessageData(): MutableLiveData<Exception> {
        return errorMessage
    }

    private val taskStatusListener: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(null)
    }

    fun getTaskStatusListenerData(): MutableLiveData<Boolean> {
        return taskStatusListener
    }

    private val isCompanyUser: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(null)
    }

    fun getIsCompanyUserData(): MutableLiveData<Boolean> {
        return isCompanyUser
    }

    private val userVehicleList: MutableLiveData<List<Quad>> by lazy {
        MutableLiveData<List<Quad>>(null)
    }

    fun getUserVehicleListData(): MutableLiveData<List<Quad>> {
        return userVehicleList
    }

    private val userRouteList: MutableLiveData<List<Route>> by lazy {
        MutableLiveData<List<Route>>(null)
    }

    fun getUserRouteListData(): MutableLiveData<List<Route>> {
        return userRouteList
    }

    private val mapPreviewPointsList: MutableLiveData<List<LatLng>> by lazy {
        MutableLiveData<List<LatLng>>(null)
    }

    fun getMapPreviewPointsListData(): MutableLiveData<List<LatLng>> {
        return mapPreviewPointsList
    }

    private val userServiceOrdersList: MutableLiveData<List<ServiceOrder>> by lazy {
        MutableLiveData<List<ServiceOrder>>()
    }

    fun getUserServiceOrdersListData(): MutableLiveData<List<ServiceOrder>> {
        return userServiceOrdersList
    }

    private val companyList: MutableLiveData<List<CompanyInfo>> by lazy {
        MutableLiveData<List<CompanyInfo>>(null)
    }

    fun getCompanyListData(): MutableLiveData<List<CompanyInfo>> {
        return companyList
    }

    private val companyListSearch: MutableLiveData<List<CompanyInfo>> by lazy {
        MutableLiveData<List<CompanyInfo>>(null)
    }

    fun getCompanyListSearchData(): MutableLiveData<List<CompanyInfo>> {
        return companyListSearch
    }

    private val loggedUserInfo: MutableLiveData<UserInfo> by lazy {
        MutableLiveData<UserInfo>(null)
    }

    fun getLoggedUserInfoData(): MutableLiveData<UserInfo> {
        return loggedUserInfo
    }

    private val orderCounter: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>(null)
    }

    private val postsList: MutableLiveData<List<PhotoPost>> by lazy {
        MutableLiveData<List<PhotoPost>>(null)
    }

    fun getPostsListData(): MutableLiveData<List<PhotoPost>> {
        return postsList
    }

    private val notificationsList: MutableLiveData<List<Notification>> by lazy {
        MutableLiveData<List<Notification>>(null)
    }

    fun getNotificationsListData(): MutableLiveData<List<Notification>> {
        return notificationsList
    }

    private val postListSearch: MutableLiveData<List<PhotoPost>> by lazy {
        MutableLiveData<List<PhotoPost>>(null)
    }

    fun getPostListSearchData(): MutableLiveData<List<PhotoPost>> {
        return postListSearch
    }

    fun resetTaskListeners() {
        errorMessage.postValue(null)
        isCompanyUser.postValue(null)
        taskStatusListener.postValue(null)
    }

    fun destroy() {
        authorizedUser.postValue(null)
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)
        isCompanyUser.postValue(null)
        userVehicleList.postValue(null)
        userRouteList.postValue(null)
        mapPreviewPointsList.postValue(null)
        userServiceOrdersList.postValue(null)
        companyList.postValue(null)
        companyListSearch.postValue(null)
        loggedUserInfo.postValue(null)
        orderCounter.postValue(null)
        postsList.postValue(null)
        notificationsList.postValue(null)
        postListSearch.postValue(null)

    }

    //function implementation ---------------------------------------

    fun registerUserAndSaveInfo(
        password: String,
        userInfo: UserInfo,
        profilePhoto: Bitmap?,
        companyInfo: CompanyInfo?
    ) {
        authorizedUser.postValue(null)
        errorMessage.postValue(null)

        val db = FirebaseFirestore.getInstance()
        if (companyInfo != null) {
            val companyRef = db.collection("serviceCompanies")
                .whereEqualTo(
                    "companyTaxIdentificationNumber",
                    companyInfo.companyTaxIdentificationNumber
                )

            companyRef.get()
                .addOnSuccessListener {
                    if (it != null && it.size() > 0) {
                        errorMessage.postValue(Exception("Firma o podanym numerze NIP już istnieje!"))
                        return@addOnSuccessListener
                    }
                }
        }

        auth.createUserWithEmailAndPassword(userInfo.emailAddress!!, password)
            .addOnSuccessListener { authResult ->
                val userUid = authResult.user!!.uid
                val storage = FirebaseStorage.getInstance()

                val filename = UUID.randomUUID().toString()
                val photoRef =
                    storage.getReference("Images/$userUid/profilePhoto/$filename")

                if (profilePhoto != null) {
                    val baos = ByteArrayOutputStream()
                    profilePhoto.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data: ByteArray = baos.toByteArray()

                    photoRef.putBytes(data)
                        .addOnSuccessListener {
                            photoRef.downloadUrl
                                .addOnSuccessListener { task ->
                                    userInfo.userProfilePhotoUrl = task.toString()
                                    userInfo.userProfilePhotoPath =
                                        "Images/$userUid/profilePhoto/$filename"

                                    uploadUserInfo(userUid, userInfo, companyInfo)
                                }
                                .addOnFailureListener { error ->
                                    errorMessage.postValue(error)
                                    return@addOnFailureListener
                                }
                        }
                        .addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                } else {
                    uploadUserInfo(userUid, userInfo, companyInfo)
                }
            }
            .addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }
    }

    private fun uploadUserInfo(uid: String, userInfo: UserInfo, companyInfo: CompanyInfo?) {
        errorMessage.postValue(null)

        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(uid)
        val companyDocRef = db.collection("serviceCompanies").document(uid)

        if (companyInfo == null) {
            userDocRef.set(userInfo)
                .addOnSuccessListener {
                    authorizedUser.postValue(auth.currentUser)
                }
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }

        } else {
            companyInfo.uid = uid
            companyDocRef.set(companyInfo)
                .addOnSuccessListener {
                    authorizedUser.postValue(auth.currentUser)
                }
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        }
    }

    fun loginUser(email: String, password: String) {
        errorMessage.postValue(null)

        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener { authResult ->
            if (authResult != null) {
                authorizedUser.postValue(authResult.user)

                val db = FirebaseFirestore.getInstance()
                val userDocRef = db.collection("users").document(authResult.user!!.uid)

                userDocRef.get().addOnSuccessListener { doc ->
                    if (doc != null) {
                        loggedUserInfo.postValue(doc.toObject(UserInfo::class.java))
                    }
                }.addOnFailureListener { e ->
                    errorMessage.postValue(e)
                    return@addOnFailureListener
                }
            }
        }.addOnFailureListener { exception ->
            errorMessage.postValue(exception)
            return@addOnFailureListener
        }
    }

    fun checkIfCompany() {
        isCompanyUser.postValue(null)
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(auth.currentUser!!.uid)
            val companyRef = db.collection("serviceCompanies").document(auth.currentUser!!.uid)

            userRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doc = task.result
                    if (doc.exists()) {
                        isCompanyUser.postValue(false)
                    } else {
                        companyRef.get().addOnCompleteListener { companyTask ->
                            if (companyTask.isSuccessful) {
                                val company = companyTask.result
                                if (company.exists()) {
                                    isCompanyUser.postValue(true)
                                } else {
                                    errorMessage.postValue(Exception("Błąd serwera"))
                                }
                            } else {
                                errorMessage.postValue(Exception("Błąd serwera"))
                            }
                        }.addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                    }
                } else {
                    errorMessage.postValue(Exception("Błąd serwera"))
                }
            }.addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun checkIfUserIsLoggedIn() {
        if (auth.currentUser != null) {
            authorizedUser.postValue(auth.currentUser)
        }
    }

    fun signOutUser() {
        if (auth.currentUser != null) {
            auth.signOut()
            authorizedUser.postValue(null)
        }
    }

    fun resetPasswordEmail(email: String) {
        errorMessage.postValue(null)
        taskStatusListener.postValue(false)

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                taskStatusListener.postValue(true)
            }
            .addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }
    }

    private fun pushNotification(notificationMsg: String) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
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
                return@addOnFailureListener
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun pushNotificationToService(notificationMsg: String, companyUid: String) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val formatted = current.format(formatter)

            val notificationKey = dbRTF.reference.child("notifications/$companyUid").push()

            notificationKey.setValue(
                Notification(
                    notificationKey.key,
                    notificationMsg,
                    formatted,
                    false
                )
            ).addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun deleteNotification(notificationKey: String) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            dbRTF.reference.child("notifications/${auth.currentUser!!.uid}/$notificationKey")
                .removeValue().addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
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
                    return@addOnFailureListener
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
                    for (notificationSnapshot in dataSnapshot.children.reversed()) {
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

    fun getOrderCounter() {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection("orders").document("orderCounter")

            ref.addSnapshotListener { value, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                if (value != null) {
                    try {
                        val counter = value.getLong("orderNumber")
                        if (counter != null) {
                            orderCounter.postValue(counter.toInt())
                        }
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }

                }
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun getVehicleList() {
        errorMessage.postValue(null)
        userVehicleList.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection("users").document(auth.currentUser!!.uid).collection("vehicles")
                .orderBy("vehicleName")


            ref.addSnapshotListener { documents, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                val quadList = mutableListOf<Quad>()
                for (document in documents!!) {
                    try {
                        quadList.add(document.toObject(Quad::class.java))
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }
                }

                userVehicleList.postValue(quadList)
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun uploadNewVehicle(quad: Quad, vehiclePhoto: Bitmap) {
        errorMessage.postValue(null)
        userVehicleList.postValue(null)

        if (auth.currentUser != null) {
            val storage = FirebaseStorage.getInstance()

            val photoRef =
                storage.getReference("Images/${auth.currentUser!!.uid}/vehiclesPhoto/${quad.vehicleVinNumber}")

            val baos = ByteArrayOutputStream()
            vehiclePhoto.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data: ByteArray = baos.toByteArray()

            photoRef.putBytes(data)
                .addOnSuccessListener {
                    photoRef.downloadUrl.addOnSuccessListener { uri ->
                        quad.vehiclePhotoUrl = uri.toString()
                        quad.vehiclePhotoStorageRef =
                            "Images/${auth.currentUser!!.uid}/vehiclesPhoto/${quad.vehicleVinNumber}"

                        uploadVehicleInfo(quad)
                    }.addOnFailureListener { error ->
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
                }
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    private fun uploadVehicleInfo(quad: Quad) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val userDocRef =
                db.collection("users").document(auth.currentUser!!.uid).collection("vehicles")
                    .document(quad.vehicleVinNumber!!)

            userDocRef.set(quad)
                .addOnSuccessListener {
                    userVehicleList.postValue(null)
                    getVehicleList()

                    taskStatusListener.postValue(true)
                }
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun deleteVehicle(vehicleVinNumber: String?) {
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()

            if (vehicleVinNumber != null) {
                val vehicleRef =
                    db.collection("users").document(auth.currentUser!!.uid).collection("vehicles")
                        .document(vehicleVinNumber)

                val orderDocs =
                    db.collection("orders")
                        .whereEqualTo("vehicle.vehicleVinNumber", vehicleVinNumber)

                orderDocs.get().addOnSuccessListener { docs ->
                    docs.forEach { doc ->
                        val orderDoc = db.collection("orders").document(doc.id)
                        orderDoc.delete()
                            .addOnFailureListener { error ->
                                errorMessage.postValue(error)
                                return@addOnFailureListener
                            }
                    }
                    vehicleRef.delete()
                        .addOnSuccessListener {
                            taskStatusListener.postValue(true)
                        }
                        .addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                }
            } else {
                errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
            }
        }
    }

    fun updateVehicle(update: MutableMap<String, Any>, vehiclePhoto: Bitmap?, vehicle: Quad) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {

            if (vehiclePhoto != null) {
                val storage = FirebaseStorage.getInstance()
                val deletion = storage.getReference(vehicle.vehiclePhotoStorageRef!!)

                deletion.delete()
                    .addOnSuccessListener {
                        val photoRef =
                            storage.getReference("Images/${auth.currentUser!!.uid}/vehiclesPhoto/${vehicle.vehicleVinNumber}")

                        val baos = ByteArrayOutputStream()
                        vehiclePhoto.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        val data: ByteArray = baos.toByteArray()

                        photoRef.putBytes(data)
                            .addOnSuccessListener {
                                photoRef.downloadUrl.addOnSuccessListener { uri ->
                                    update["vehiclePhotoUrl"] = uri.toString()
                                    update["vehiclePhotoStorageRef"] =
                                        "Images/${auth.currentUser!!.uid}/vehiclesPhoto/${vehicle.vehicleVinNumber}"

                                    updateOrdersAndVehicle(update, vehicle)
                                }.addOnFailureListener { error ->
                                    errorMessage.postValue(error)
                                    return@addOnFailureListener
                                }
                            }
                            .addOnFailureListener { error ->
                                errorMessage.postValue(error)
                                return@addOnFailureListener
                            }
                    }
                    .addOnFailureListener { error ->
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
            } else {
                updateOrdersAndVehicle(update, vehicle)
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    private fun updateOrdersAndVehicle(update: MutableMap<String, Any>, vehicle: Quad) {
        taskStatusListener.postValue(null)
        errorMessage.postValue(null)

        val db = FirebaseFirestore.getInstance()

        val orderDocs = db.collection("orders")
            .whereEqualTo("vehicle.vehicleVinNumber", vehicle.vehicleVinNumber)

        orderDocs.get()
            .addOnSuccessListener { docs ->
                docs.forEach { _doc ->
                    val doc = db.collection("orders").document(_doc.id)
                    doc.update("vehicle", update)
                        .addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                }

                val userDocRef =
                    db.collection("users")
                        .document(auth.currentUser!!.uid)
                        .collection("vehicles")
                        .document(vehicle.vehicleVinNumber!!)

                userDocRef.update(update)
                    .addOnSuccessListener {
                        taskStatusListener.postValue(true)
                    }
                    .addOnFailureListener { error ->
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
            }
            .addOnFailureListener { error ->
                errorMessage.postValue(error)
                return@addOnFailureListener
            }
    }

    fun getCompaniesList() {
        errorMessage.postValue(null)
        companyList.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection("serviceCompanies")


            ref.addSnapshotListener { documents, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                val list = mutableListOf<CompanyInfo>()
                for (document in documents!!) {
                    try {
                        val company = document.toObject(CompanyInfo::class.java)
                        if (company.isVerified) {
                            list.add(company)
                        }
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }
                }

                companyList.postValue(list)
            }
        } else {
            errorMessage.postValue(Exception("Wsystąpił nieznany błąd."))
        }
    }

    fun getCompaniesListSearch(
        text: String,
        category: SearchParam
    ) {
        val list = companyList.value

        if (text.isNotEmpty() && list != null) {
            when (category) {
                SearchParam.NAME -> {
                    companyListSearch.postValue(list.filter {
                        it.companyName!!.contains(text, true)
                    })
                }
                SearchParam.ADDRESS -> {
                    companyListSearch.postValue(list.filter {
                        it.companyAddress!!.contains(text, true)
                    })
                }
                SearchParam.POSTAL_CODE -> {
                    companyListSearch.postValue(list.filter {
                        it.companyPostalCode!!.contains(text, true)
                    })
                }
                SearchParam.CITY -> {
                    companyListSearch.postValue(list.filter {
                        it.companyCity!!.contains(text, true)
                    })
                }
                SearchParam.PHONE -> {
                    companyListSearch.postValue(list.filter {
                        it.companyPhoneNumber!!.contains(text, true)
                    })
                }
            }
        } else {
            companyListSearch.postValue(list)
        }
    }

    fun getPostSearchList(text: String?) {
        val list = postsList.value

        if (text != null && text.isNotEmpty() && list != null) {
            postListSearch.postValue(list.filter {
                if (it.vehicle == null) {
                    it.tags!!.contains(text, true)
                } else {
                    it.tags!!.contains(text, true)
                        .or(it.vehicle!!.vehicleColor?.contains(text, true) == true)
                        .or(it.vehicle!!.vehicleModel?.contains(text, true) == true)
                        .or(it.vehicle!!.vehicleName?.contains(text, true) == true).or(
                            it.vehicle!!.vehicleManufacturer?.contains(
                                text, true
                            ) == true
                        ).or(it.vehicle!!.vehicleEngineCapacity?.toString()!!.contains(text, true))
                }

            })
        } else {
            postListSearch.postValue(list)
        }
    }

    fun getCurrentUserInfo() {
        errorMessage.postValue(null)
        loggedUserInfo.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(auth.currentUser!!.uid)


            docRef.addSnapshotListener { document, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                if (document != null) {
                    try {
                        loggedUserInfo.postValue(document.toObject(UserInfo::class.java))
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }
                }
            }
        } else {
            errorMessage.postValue(Exception("Wsystąpił nieznany błąd."))
        }
    }

    fun updateProfileInfo(
        update: MutableMap<String, Any>,
        selectedBitmap: Bitmap?,
        userInfo: UserInfo
    ) {
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)

        if (auth.currentUser != null) {
            if (selectedBitmap != null) {
                val storage = FirebaseStorage.getInstance()
                if(!userInfo.userProfilePhotoPath.isNullOrEmpty()) {
                    storage.getReference(userInfo.userProfilePhotoPath!!).delete()
                        .addOnSuccessListener {
                            val filename = UUID.randomUUID().toString()
                            val newPhotoRef =
                                storage.getReference("Images/${auth.currentUser!!.uid}/profilePhoto/$filename")

                            val baos = ByteArrayOutputStream()
                            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                            val data: ByteArray = baos.toByteArray()

                            newPhotoRef.putBytes(data)
                                .addOnSuccessListener {
                                    newPhotoRef.downloadUrl.addOnSuccessListener { uri ->
                                        update["userProfilePhotoUrl"] = uri.toString()
                                        update["userProfilePhotoPath"] =
                                            "Images/${auth.currentUser!!.uid}/profilePhoto/$filename"



                                        FirebaseFirestore.getInstance().collection("users")
                                            .document(auth.currentUser!!.uid).update(update)
                                            .addOnSuccessListener {
                                                updatePhotoLinks(uri.toString())
                                                taskStatusListener.postValue(true)
                                            }
                                            .addOnFailureListener { error ->
                                                taskStatusListener.postValue(false)
                                                errorMessage.postValue(error)
                                                return@addOnFailureListener
                                            }
                                    }
                                }.addOnFailureListener { error ->
                                    taskStatusListener.postValue(false)
                                    errorMessage.postValue(error)
                                    return@addOnFailureListener
                                }
                        }.addOnFailureListener { error ->
                            taskStatusListener.postValue(false)
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                } else {
                    val filename = UUID.randomUUID().toString()
                    val newPhotoRef =
                        storage.getReference("Images/${auth.currentUser!!.uid}/profilePhoto/$filename")

                    val baos = ByteArrayOutputStream()
                    selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data: ByteArray = baos.toByteArray()

                    newPhotoRef.putBytes(data)
                        .addOnSuccessListener {
                            newPhotoRef.downloadUrl.addOnSuccessListener { uri ->
                                update["userProfilePhotoUrl"] = uri.toString()
                                update["userProfilePhotoPath"] =
                                    "Images/${auth.currentUser!!.uid}/profilePhoto/$filename"


                                FirebaseFirestore.getInstance().collection("users")
                                    .document(auth.currentUser!!.uid).update(update)
                                    .addOnSuccessListener {
                                        updatePhotoLinks(uri.toString())
                                        taskStatusListener.postValue(true)
                                    }
                                    .addOnFailureListener { error ->
                                        taskStatusListener.postValue(false)
                                        errorMessage.postValue(error)
                                        return@addOnFailureListener
                                    }
                            }
                        }.addOnFailureListener { error ->
                            taskStatusListener.postValue(false)
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                }
            } else {
                FirebaseFirestore.getInstance().collection("users").document(auth.currentUser!!.uid)
                    .update(update).addOnSuccessListener {
                        taskStatusListener.postValue(true)
                    }.addOnFailureListener { error ->
                        taskStatusListener.postValue(false)
                        errorMessage.postValue(error)
                        return@addOnFailureListener
                    }
            }
        }
        else {
            errorMessage.postValue(Exception("Wsystąpił nieznany błąd."))
        }
    }

    private fun updatePhotoLinks(photoUrl: String) {
        if(auth.currentUser != null){
            val postRef = dbFF.collection("posts").whereEqualTo("authorUid", auth.currentUser!!.uid)

            postRef.get().addOnSuccessListener { docs ->
                docs.forEach { _doc ->
                    val doc = dbFF.collection("posts").document(_doc.id)
                    doc.update(mapOf(
                        "authorPhotoUrl" to photoUrl
                    ))
                }
            }

            val users = dbFF.collection("users")

            users.get().addOnSuccessListener { docs ->
                docs.forEach { _doc ->
                    val doc = dbFF.collection("users").document(_doc.id).collection("conversations").whereEqualTo("senderUid", auth.currentUser!!.uid)
                    doc.get().addOnSuccessListener { docs2 ->
                        docs2.forEach { _doc2 ->
                            val conv = dbFF.collection("users").document(_doc.id).collection("conversations").document(_doc2.id)
                            conv.update(mapOf(
                                "senderPhotoUrl" to photoUrl
                            ))
                        }
                    }
                }
            }
        }
    }

    fun saveServiceOrder(order: ServiceOrder) {
        taskStatusListener.postValue(null)
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val docName =
                order.companyTaxIdentityNumber + "_" + order.customerUid + "_no" + orderCounter.value?.plus(
                    1
                )
            val orderRef =
                db.collection("orders").document(docName)

            order.orderNumber = orderCounter.value?.plus(1)!!

            orderRef.set(order)
                .addOnSuccessListener {
                    pushNotification("Wysłano zgłoszenie serwisowe. Nr zamówienia: ${order.orderNumber}")
                    pushNotificationToService(
                        "Nowe zamówienie serwisowe. Numer zamówienia: ${order.orderNumber}",
                        order.companyUid
                    )
                    val counterRef = db.collection("orders").document("orderCounter")

                    counterRef.update("orderNumber", orderCounter.value?.plus(1))
                        .addOnSuccessListener {
                            taskStatusListener.postValue(true)
                        }
                        .addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                }
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

    fun getUserServiceOrdersList() {
        errorMessage.postValue(null)
        userServiceOrdersList.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val orderRef =
                db.collection("orders").whereEqualTo("customerUid", auth.currentUser!!.uid)


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
                            pushNotification(
                                "Zmiana statusu zamówienia nr: ${order.orderNumber} na: ${
                                    order.status?.get(
                                        order.status!!.lastIndex
                                    )?.orderStatus
                                }"
                            )
                        } catch (error: Exception) {
                            errorMessage.postValue(error)
                        }
                    }
                }

                userServiceOrdersList.postValue(list)
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

    fun uploadPhotoPost(photoBitmap: Bitmap, vehicle: Quad?, tags: String) {
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)

        if (auth.currentUser != null && loggedUserInfo.value != null) {
            val db = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            val filename = UUID.randomUUID().toString()
            val photoRef = storage.getReference("posts/$filename")
            val postRef = db.collection("posts")

            val baos = ByteArrayOutputStream()
            photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data: ByteArray = baos.toByteArray()

            photoRef.putBytes(data)
                .addOnSuccessListener {
                    photoRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            val post = PhotoPost(
                                "${loggedUserInfo.value!!.firstName} ${loggedUserInfo.value!!.lastName}",
                                loggedUserInfo.value!!.userProfilePhotoUrl,
                                uri.toString(),
                                "posts/$filename",
                                0,
                                vehicle,
                                tags,
                                auth.currentUser!!.uid
                            )
                            postRef.add(post)
                                .addOnSuccessListener {
                                    it.update(
                                        mapOf("documentId" to it.id)
                                    )
                                        .addOnSuccessListener {
                                            taskStatusListener.postValue(true)
                                        }.addOnFailureListener { error ->
                                            errorMessage.postValue(error)
                                            return@addOnFailureListener
                                        }
                                }
                                .addOnFailureListener { error ->
                                    errorMessage.postValue(error)
                                    return@addOnFailureListener
                                }
                        }
                        .addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                }
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

    fun getPostsList() {
        errorMessage.postValue(null)
        postsList.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val postsRef =
                db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING)


            postsRef.addSnapshotListener { documents, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                val list = mutableListOf<PhotoPost>()
                for (document in documents!!) {
                    try {
                        val order = document.toObject(PhotoPost::class.java)
                        list.add(order)
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }
                }

                postsList.postValue(list)
            }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd"))
        }
    }

    fun updatePosts(photoPostChanges: MutableList<PhotoPostChanges>) {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()

            for (change in photoPostChanges) {
                val post = postsList.value?.first {
                    it.documentId == change.documentId
                }

                if (post != null) {
                    var likedBy = mutableListOf<String>()

                    if (post.likedBy != null) {
                        likedBy = post.likedBy as MutableList<String>
                    }

                    if (change.likes == 1) {
                        likedBy.add(auth.currentUser!!.uid)
                    } else if (change.likes == -1) {
                        likedBy.remove(auth.currentUser!!.uid)
                    }

                    val postsRef =
                        db.collection("posts").document(change.documentId)

                    if (likedBy.isNotEmpty()) {
                        postsRef.update(
                            mapOf(
                                "likedBy" to (likedBy as List<String>),
                                "likesCounter" to (post.likesCounter + change.likes)
                            )
                        ).addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                    } else {
                        postsRef.update(
                            mapOf(
                                "likedBy" to (likedBy as List<String>),
                                "likesCounter" to 0
                            )
                        ).addOnFailureListener { error ->
                            errorMessage.postValue(error)
                            return@addOnFailureListener
                        }
                    }
                }
            }
        } else {
            errorMessage.postValue(Exception("Wystapił nieznany błąd."))
        }
    }

    fun getUserRoutesList() {
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection("users").document(auth.currentUser!!.uid).collection("routes")
                .orderBy("name")

            ref.addSnapshotListener { documents, error ->
                if (error != null) {
                    errorMessage.postValue(error)
                    return@addSnapshotListener
                }

                val routeList = mutableListOf<Route>()
                for (document in documents!!) {
                    try {
                        val route = document.toObject(Route::class.java)
                        routeList.add(route)
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }
                }

                userRouteList.postValue(routeList)
            }
        } else {
            errorMessage.postValue(Exception("Wystapił nieznany błąd."))
        }
    }

    fun getMapPreviewArrayOfPoints(routeName: String) {
        errorMessage.postValue(null)
        mapPreviewPointsList.postValue(null)

        if (auth.currentUser != null) {
            val storage = FirebaseStorage.getInstance()
            storage.reference.child("routes/${auth.uid}/${routeName}")
                .getBytes(Long.MAX_VALUE).addOnSuccessListener { input ->
                    val parserFactory: XmlPullParserFactory
                    try {
                        parserFactory = XmlPullParserFactory.newInstance()
                        val parser: XmlPullParser = parserFactory.newPullParser()
                        val stream: InputStream = input.inputStream()
                        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        parser.setInput(stream, null)

                        var eventType: Int = parser.eventType

                        val pointsList = mutableListOf<LatLng>()

                        while (eventType != XmlPullParser.END_DOCUMENT) {

                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    val text = parser.name

                                    if (text.equals("wpt")) {
                                        val lat = parser.getAttributeValue(null, "lat").toDouble()
                                        val lon = parser.getAttributeValue(null, "lon").toDouble()
                                        val coordinate = LatLng(lat, lon)
                                        pointsList.add(coordinate)
                                    }
                                }
                            }
                            eventType = parser.next()
                        }

                        mapPreviewPointsList.postValue(pointsList)
                    } catch (error: Exception) {
                        errorMessage.postValue(error)
                    }
                }.addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun saveRouteOnlyUpdateMileage(vehicle: Quad, distance: Float) {
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val docRef =
                db.collection("users").document(auth.currentUser!!.uid).collection("vehicles")
                    .document(vehicle.vehicleVinNumber!!)

            val oldMileage = userVehicleList.value?.first {
                it.vehicleVinNumber == vehicle.vehicleVinNumber
            }

            val newMileage = oldMileage?.vehicleCurrentMileage?.plus(distance)

            docRef.update(
                mapOf(
                    "vehicleCurrentMileage" to newMileage
                )
            )
                .addOnSuccessListener {
                    taskStatusListener.postValue(true)
                }
                .addOnFailureListener { error ->
                    errorMessage.postValue(error)
                    return@addOnFailureListener
                }
        } else {
            errorMessage.postValue(Exception("Wystąpił nieznany błąd."))
        }
    }

    fun saveRouteAll(route: Route, points: List<LatLng>) {
        errorMessage.postValue(null)
        taskStatusListener.postValue(null)

        if (auth.currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val userDocRef =
                db.collection("users").document(auth.currentUser!!.uid)
                    .collection("routes")
                    .document(route.name!!)

            userDocRef.get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val docSnapshot = task.result
                        if (docSnapshot.exists()) {
                            taskStatusListener.postValue(false)
                            return@addOnCompleteListener
                        } else {
                            var data: String =
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<gpx creator=\"qcteam.quadclub.com\" version=\"1.0\">\n<name>${route.name}</name>\n<author>${auth.currentUser!!.uid}</author>\n"

                            for (i in points) {
                                data += "<wpt lat=\"${i.latitude}\" lon=\"${i.longitude}\">\n</wpt>\n"
                            }

                            data += "</gpx>"

                            val storage = FirebaseStorage.getInstance()

                            val routeRef =
                                storage.getReference("/routes/${auth.currentUser!!.uid}/${route.name}")

                            routeRef.putBytes(data.toByteArray(Charset.defaultCharset()))
                                .addOnSuccessListener {
                                    routeRef.downloadUrl.addOnSuccessListener { uri ->
                                        route.routeUrl = uri.toString()

                                        userDocRef.set(route)
                                            .addOnSuccessListener {
                                                taskStatusListener.postValue(true)
                                            }
                                            .addOnFailureListener { error ->
                                                errorMessage.postValue(error)
                                                return@addOnFailureListener
                                            }
                                    }
                                }
                                .addOnFailureListener { error ->
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

    fun deleteRoute(routeName: String) {
        taskStatusListener.postValue(null)
        errorMessage.postValue(null)

        if (auth.currentUser != null) {
            val storage = FirebaseStorage.getInstance()
            val db = FirebaseFirestore.getInstance()

            val routeRef =
                storage.getReference("routes/${auth.currentUser!!.uid}/$routeName")
            val userDocRef =
                db.collection("users").document(auth.currentUser!!.uid)
                    .collection("routes")
                    .document(routeName)

            routeRef.delete().addOnSuccessListener {
                userDocRef.delete().addOnSuccessListener {
                    taskStatusListener.postValue(true)
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



}