package com.qcteam.quadclub.data

data class CompanyInfo(
    var uid: String? = null,
    var userInfo: UserInfo? = null,
    var companyName: String? = null,
    var companyTaxIdentificationNumber: String? = null,
    var companyAddress: String? = null,
    var companyPostalCode: String? = null,
    var companyCity: String? = null,
    var companyPhoneNumber: String? = null,
    @field:JvmField
    var isVerified: Boolean = false,
)
