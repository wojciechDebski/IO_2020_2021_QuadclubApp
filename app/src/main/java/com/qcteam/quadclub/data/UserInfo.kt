package com.qcteam.quadclub.data

data class UserInfo(
    var userProfilePhotoUrl : String? = null,
    var userProfilePhotoPath : String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var emailAddress: String? = null,
    var phoneNumber: String? = null,
    var address: String? = null,
    var postalCode: String? = null,
    var city: String? = null,
    @field:JvmField
    var isService: Boolean = false
)

data class UserInfoForConversations (
    val userProfilePhotoUrl: String? = null,
    val userNameSurname: String? = null,
    val userUid: String? = null
)
