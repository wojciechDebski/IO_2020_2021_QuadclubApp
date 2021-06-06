package com.qcteam.quadclub.data.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.ServiceOrder
import com.qcteam.quadclub.data.ServiceOrderStatus
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_ORDER_NUMBER

class OrdersAdapter(private val ordersList: List<ServiceOrder>) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.recycler_order_card, parent, false)

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
        View.OnClickListener {

        private var view: View = v
        private lateinit var order: ServiceOrder

        private var orderLayout: ConstraintLayout = view.findViewById(R.id.order_constraint_layout)
        private var otherDateLayout: ConstraintLayout = view.findViewById(R.id.other_date_select_layout)
        private var orderCard: CardView = view.findViewById(R.id.order_card)

        private var orderNo: TextView = view.findViewById(R.id.service_order_no_text)
        private var orderDateTime: TextView = view.findViewById(R.id.service_date_time_text)
        private var orderVehicleName: TextView = view.findViewById(R.id.company_vehicle_name_text)
        private var orderVehicleModel: TextView = view.findViewById(R.id.company_vehicle_model_text)
        private var orderStatus: TextView = view.findViewById(R.id.service_status_text)

        private var otherDateSpinner : Spinner = view.findViewById(R.id.another_dates_spinner)
        private var otherDateSelectBtn : Button = view.findViewById(R.id.accept_another_date_button)

        private var companyName: TextView = view.findViewById(R.id.service_company_name_text)
        private var companyAddress: TextView = view.findViewById(R.id.service_company_address_text)
        private var companyPostalCode: TextView = view.findViewById(R.id.service_company_postal_code_text)
        private var companyCity: TextView = view.findViewById(R.id.service_company_city_text)
        private var companyPhone: TextView = view.findViewById(R.id.service_company_phone_text)


        init {
            orderCard.setOnClickListener(this)
        }


        override fun onClick(v: View?) {
            val bundle = bundleOf(
                PARAM_ORDER_NUMBER to order.orderNumber.toString(),
            )
            view.findNavController()
                .navigate(R.id.action_serviceOrderListFragment_to_serviceOrderMoreInfoFragment, bundle)
        }


        fun bindOrder(item: ServiceOrder) {
            this.order = item

            if(!order.customerAccept || !order.serviceAccept){
                orderLayout.setBackgroundResource(R.drawable.service_order_pending_background)
            } else if (order.orderInProgress) {
                orderLayout.setBackgroundResource(R.drawable.service_order_in_progress_background)
            } else if (order.orderDone) {
                orderLayout.setBackgroundResource(R.drawable.service_order_done_background)
            }

            if(order.serviceSuggestedOtherDates && order.otherDatesList != null && order.otherDatesList!!.isNotEmpty()){
                otherDateLayout.visibility = View.VISIBLE
                otherDateSpinner.adapter = ArrayAdapter(
                    view.context,
                    android.R.layout.simple_list_item_1,
                    order.otherDatesList!!
                )
                otherDateSelectBtn.setOnClickListener {
                    val db = FirebaseFirestore.getInstance()
                    val ref = db.collection("orders").document(order.companyTaxIdentityNumber + "_" + order.customerUid + "_no" + order.orderNumber)
                    val status = mutableListOf<ServiceOrderStatus>()
                    order.status?.forEach {
                        status.add(it)
                    }
                    status.add(ServiceOrderStatus("Klient zaakceptował zaproponowaną datę"))
                    val update = mapOf(
                        "dateTime" to order.otherDatesList!![otherDateSpinner.selectedItemPosition],
                        "serviceSuggestedOtherDates" to false,
                        "otherDatesList" to null,
                        "serviceAccept" to false,
                        "customerAccept" to true,
                        "status" to status,
                        "customerSelectedOtherDate" to true
                    )

                    ref.update(update)
                        .addOnSuccessListener {
                            val builder = AlertDialog.Builder(view.context)
                            builder.setTitle("Zmiana daty i godziny serwisu")
                            builder.setMessage("Wybrana przez ciebie data została przekazana do serwisu.")
                            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                            }
                            builder.show()
                        }
                        .addOnFailureListener { error ->
                            AlertBuilder.buildErrorAlert(view.context, error)
                        }

                }
            } else {
                otherDateLayout.visibility = View.GONE
            }

            companyName.text = order.companyName
            companyAddress.text = order.companyAddress
            companyPostalCode.text = order.companyPostalCode
            companyCity.text = order.companyCity
            companyPhone.text = order.companyPhone
            orderNo.text = order.orderNumber.toString()
            orderDateTime.text = order.dateTime.toString()
            orderVehicleName.text = order.vehicle?.vehicleName ?: ""
            orderVehicleModel.text = order.vehicle?.vehicleModel ?: ""
            if (order.status != null){
                orderStatus.text = order.status!!.get(order.status!!.lastIndex).orderStatus
            }

        }

    }

}
