package com.qcteam.quadclub.data

import com.google.firebase.Timestamp

data class ServiceOrderStatus(
    var orderStatus: String? = null,
    var date: Timestamp = Timestamp.now()
)