package com.qcteam.quadclub.data.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.ServiceOrder
import com.qcteam.quadclub.data.helpers.PARAM_ORDER_NUMBER

class OrdersAdapterInVehicle(private val ordersList: List<ServiceOrder>) : RecyclerView.Adapter<OrdersAdapterInVehicle.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.recycler_order_card_in_vehicle, parent, false)

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
        private var orderCard: CardView = view.findViewById(R.id.order_card)

        private var orderNo: TextView = view.findViewById(R.id.service_order_no_text)
        private var orderDateTime: TextView = view.findViewById(R.id.service_date_time_text)
        private var orderStatus: TextView = view.findViewById(R.id.service_status_text)


        init {
            orderCard.setOnClickListener(this)
        }


        override fun onClick(v: View?) {
            val bundle = bundleOf(
                PARAM_ORDER_NUMBER to order.orderNumber.toString(),
            )
            view.findNavController()
                .navigate(R.id.action_vehicleMoreInfoFragment_to_serviceOrderMoreInfoFragment, bundle)
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

            orderNo.text = order.orderNumber.toString()
            orderDateTime.text = order.dateTime.toString()
            if (order.status != null){
                orderStatus.text = order.status!![order.status!!.lastIndex].orderStatus
            }

        }

    }

}
