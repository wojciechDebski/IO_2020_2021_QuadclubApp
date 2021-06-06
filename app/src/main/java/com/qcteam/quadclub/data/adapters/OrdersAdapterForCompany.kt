package com.qcteam.quadclub.data.adapters

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.ServiceOrder
import com.qcteam.quadclub.data.ServiceOrderStatus
import com.qcteam.quadclub.data.helpers.PARAM_ORDER_NUMBER
import java.util.*

class OrdersAdapterForCompany(private val ordersList: List<ServiceOrder>) :
    RecyclerView.Adapter<OrdersAdapterForCompany.OrderViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.recycler_order_card_company, parent, false)

        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = ordersList[position]
        holder.bindOrder(item)
    }

    override fun getItemCount(): Int {
        return ordersList.size
    }

    class OrderViewHolder(v: View) : RecyclerView.ViewHolder(v),
        View.OnClickListener, DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

        private val db = FirebaseFirestore.getInstance()

        private var view: View = v
        private lateinit var order: ServiceOrder

        private var orderLayout: ConstraintLayout = view.findViewById(R.id.order_constraint_layout)
        private var acceptOrderLayout: ConstraintLayout = view.findViewById(R.id.accept_order_layout)
        private var orderCard: CardView = view.findViewById(R.id.order_card)

        private var orderNo: TextView = view.findViewById(R.id.service_order_no_text)
        private var orderDateTime: TextView = view.findViewById(R.id.service_date_time_text)
        private var orderVehicleModel: TextView = view.findViewById(R.id.company_vehicle_model_text)
        private var orderStatus: TextView = view.findViewById(R.id.service_status_text)
        private var customer: TextView = view.findViewById(R.id.service_vehicle_owner_text)
        private var customerPhone: TextView = view.findViewById(R.id.service_vehicle_owner_phone_text)
        private var reportReason: TextView = view.findViewById(R.id.service_vehicle_report_reason_text)

        private var acceptOrder: Button = view.findViewById(R.id.accept_order_button)
        private var suggestOtherDate: Button = view.findViewById(R.id.change_date_time_button)

        private val suggestionList = mutableListOf<String>()
        private var dateString = ""

        private var dialog: View = LayoutInflater.from(view.context)
            .inflate(R.layout.company_suggest_other_data_time_dialog, null)
        private var dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(view.context)
            .setView(dialog)
        private var suggestionListView: ListView = dialog.findViewById(R.id.other_date_list_view)
        private var addDateTimeBtn: ImageButton = dialog.findViewById(R.id.add_date_time_suggestion_button)
        private var closeDialogBtn: ImageButton = dialog.findViewById(R.id.add_suggestion_date_time_close_dialog)
        private var send: Button = dialog.findViewById(R.id.send_suggestion_button)


        init {
            orderCard.setOnClickListener(this)
        }


        override fun onClick(v: View?) {
            val bundle = bundleOf(
                PARAM_ORDER_NUMBER to order.orderNumber.toString()
            )
            view.findNavController()
                .navigate(R.id.action_companyOrdersFragment_to_companyOrderDetailsFragment, bundle)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun bindOrder(item: ServiceOrder) {
            this.order = item

            if (!order.customerAccept || !order.serviceAccept) {
                orderLayout.setBackgroundResource(R.drawable.service_order_pending_background)
            } else if (order.orderInProgress) {
                orderLayout.setBackgroundResource(R.drawable.service_order_in_progress_background)
            } else if (order.orderDone) {
                orderLayout.setBackgroundResource(R.drawable.service_order_done_background)
            }

            if(order.serviceSuggestedOtherDates){
                suggestOtherDate.isEnabled = false
            }

            if (order.orderInProgress || order.orderDone) {
                acceptOrderLayout.visibility = View.GONE
            } else {
                acceptOrderLayout.visibility = View.VISIBLE
                acceptOrder.setOnClickListener {
                    val status = mutableListOf<ServiceOrderStatus>()
                    order.status?.forEach {
                        status.add(it)
                    }
                    status.add(ServiceOrderStatus("Zgłoszenie zaakceptowane przez serwis"))

                    val update = mapOf(
                        "serviceAccept" to true,
                        "orderInProgress" to true,
                        "status" to status
                    )
                    val orderRef = db.collection("orders")
                        .document(order.companyTaxIdentityNumber + "_" + order.customerUid + "_no" + order.orderNumber)

                    orderRef.update(update)
                        .addOnSuccessListener {
                            val builder = AlertDialog.Builder(view.context)
                            builder.setTitle("Akceptacja zgłoszenia")
                            builder.setMessage("Klient otrzymał potwierdzenie akceptacji zgłoszenia")
                            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                            }
                            builder.show()
                        }
                        .addOnFailureListener {
                            val builder = AlertDialog.Builder(view.context)
                            builder.setTitle("Błąd")
                            builder.setMessage(it.localizedMessage)
                            builder.setPositiveButton(android.R.string.cancel) { _, _ ->
                            }
                            builder.show()
                        }
                }

                suggestOtherDate.setOnClickListener {
                    val mAlertDialog = dialogBuilder.show()

                    addDateTimeBtn.setOnClickListener {
                        val calendar: Calendar = Calendar.getInstance()
                        val datePickerDialog =
                            DatePickerDialog(
                                view.context,
                                this,
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                        datePickerDialog.datePicker.minDate = calendar.time.time
                        datePickerDialog.show()
                    }

                    closeDialogBtn.setOnClickListener {
                        suggestionList.clear()
                        mAlertDialog.cancel()
                    }

                    send.setOnClickListener {
                        val status = mutableListOf<ServiceOrderStatus>()
                        order.status?.forEach {
                            status.add(it)
                        }
                        status.add(ServiceOrderStatus("Serwis zaproponował inne daty realizacji zamówienia"))
                        val update = mapOf(
                            "serviceAccept" to true,
                            "customerAccept" to false,
                            "serviceSuggestedOtherDates" to true,
                            "otherDatesList" to suggestionList,
                            "status" to status
                        )
                        val orderRef = db.collection("orders")
                            .document(order.companyTaxIdentityNumber + "_" + order.customerUid + "_no" + order.orderNumber)

                        orderRef.update(update)
                            .addOnSuccessListener {
                                val builder = AlertDialog.Builder(view.context)
                                builder.setTitle("Dodano opcjonalne terminy")
                                builder.setMessage("Klient otrzymał propozycje dodanych przez Ciebie terminów.")
                                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                                }
                                builder.show()
                            }
                            .addOnFailureListener {
                                val builder = AlertDialog.Builder(view.context)
                                builder.setTitle("Błąd")
                                builder.setMessage(it.localizedMessage)
                                builder.setPositiveButton(android.R.string.cancel) { _, _ ->
                                }
                                builder.show()
                            }
                        suggestionList.clear()
                        mAlertDialog.cancel()
                    }

                }
            }


            orderNo.text = order.orderNumber.toString()
            orderDateTime.text = order.dateTime
            orderVehicleModel.text = order.vehicle?.vehicleModel
            customer.text = order.customerNameSurname
            customerPhone.text = order.customerPhone
            reportReason.text = order.reportReason
            if (order.status != null) {
                orderStatus.text = order.status!![order.status!!.lastIndex].orderStatus
            }

        }


        override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
            dateString = "$dayOfMonth.$month.$year"

            val calendar: Calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(
                view?.context,
                this,
                calendar.get(Calendar.HOUR),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        }

        override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
            val dateTime = if(minute == 0){
                "$dateString godz. $hourOfDay:$minute" + "0"
            } else {
                "$dateString godz. $hourOfDay:$minute"
            }

            suggestionList.add(dateTime)
            val adapter = view?.let {
                ArrayAdapter(
                    it.context,
                    android.R.layout.simple_list_item_1,
                    suggestionList
                )
            }
            if (suggestionList.size >= 2){
                send.isEnabled = true
            }
            adapter?.notifyDataSetChanged()
            suggestionListView.adapter = adapter
        }

    }

}