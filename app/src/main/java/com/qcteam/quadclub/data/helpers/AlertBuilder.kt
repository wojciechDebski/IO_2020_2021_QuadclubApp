package com.qcteam.quadclub.data.helpers

import android.app.AlertDialog
import android.content.Context
import com.qcteam.quadclub.R

object AlertBuilder {

    fun buildNetworkAlert(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.resources.getString(R.string.network_alert_title))
        builder.setMessage(context.resources.getString(R.string.network_alert_message))
        builder.setPositiveButton(android.R.string.ok) { _, _ -> }
        builder.show()
    }

    fun buildErrorAlert(context: Context, error: Exception){
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.resources.getString(R.string.error_alert_title))
        builder.setMessage(error.localizedMessage)
        builder.setPositiveButton(android.R.string.ok) { _, _ -> }
        builder.show()
    }

    fun buildEmptyVehicleListAlert(context: Context){
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.resources.getString(R.string.caution_alert_title))
        builder.setMessage(context.resources.getString(R.string.empty_vehicle_colection_appoitment))
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
        }
        builder.show()
    }

    fun buildEmptyImageViewAlert(context: Context){
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.resources.getString(R.string.caution_alert_title))
        builder.setMessage(context.resources.getString(R.string.empty_image_view_appoitment))
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
        }
        builder.show()
    }


}