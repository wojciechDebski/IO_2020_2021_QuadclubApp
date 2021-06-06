package com.qcteam.quadclub.data

data class Quad(
    val vehicleName: String? = null,
    val vehicleModel: String? = null,
    val vehicleVinNumber: String? = null,
    val vehicleCurrentMileage: Float? = null,
    var vehiclePhotoUrl: String? = null,
    var vehiclePhotoStorageRef: String? = null,
    val vehicleManufacturer: String? = null,
    val vehicleEngineCapacity: Float? = null,
    val vehicleColor: String? = null
) {
    fun vehicleCurrentMileage_toString(): String {
        return String.format("%.2f", vehicleCurrentMileage) + "km"
    }

    fun vehicleEngineCapacity_toString(): String {
        return if (vehicleEngineCapacity != null) {
            "$vehicleEngineCapacity cm3"
        } else {
            "brak informacji"
        }
    }


}
