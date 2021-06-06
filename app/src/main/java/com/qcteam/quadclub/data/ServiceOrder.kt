package com.qcteam.quadclub.data

data class ServiceOrder(
    var orderNumber: Int = 0,
    var companyUid: String = "",
    var companyName: String = "",
    var companyAddress: String = "",
    var companyPostalCode: String = "",
    var companyCity: String = "",
    var companyNip: String = "",
    var companyPhone: String = "",
    var customerUid: String = "",
    var customerNameSurname: String? = null,
    val customerPhone: String? = null,
    var companyTaxIdentityNumber: String = "",
    var dateTime: String? = null,
    var reportReason: String = "",
    var additionalInfo: String = "",
    var vehicle : Quad? = null,
    var status : List<ServiceOrderStatus>? = null,
    var customerAccept: Boolean = true,
    var serviceAccept: Boolean = false,
    var orderInProgress: Boolean = false,
    var orderDone: Boolean = false,
    var serviceInformation: String = "",
    var serviceSuggestedOtherDates: Boolean = false,
    val otherDatesList: List<String>? = null,
    val customerSelectedOtherDate: Boolean = false
)
