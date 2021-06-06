package com.qcteam.quadclub.data.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.ServiceOrderStatus
import java.text.DateFormat
import java.util.*


class StatusListAdapter(private val statusList: List<ServiceOrderStatus>) : RecyclerView.Adapter<StatusListAdapter.OrderViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.recycler_status_card, parent, false)

        return OrderViewHolder(view,statusList)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = statusList[position]
        holder.bindOrder(item , position)
    }

    override fun getItemCount(): Int {
        return statusList.size
    }

    class OrderViewHolder(v: View, statusList: List<ServiceOrderStatus>) : RecyclerView.ViewHolder(v) {

        private var view: View = v
        private lateinit var serviceStatus: ServiceOrderStatus
        private var statusListSize = 0

        private var topLine: ImageView
        private var bottomLine: ImageView
        private var statusCircle: ImageView
        private var statusText: TextView
        private var statusTime: TextView


        init {
            statusListSize = statusList.size

            topLine = view.findViewById(R.id.status_top_line)
            bottomLine = view.findViewById(R.id.status_bottom_line)
            statusCircle = view.findViewById(R.id.status_circle)
            statusText = view.findViewById(R.id.status_text)
            statusTime = view.findViewById(R.id.status_date)

        }


        fun bindOrder(item: ServiceOrderStatus, position: Int) {
            this.serviceStatus = item

            if(statusListSize > 1) {
                if (position == 0) {
                    topLine.visibility = View.INVISIBLE
                    statusCircle.setImageResource(R.drawable.orange_circle)
                } else if (position == statusListSize - 1) {
                    bottomLine.visibility = View.INVISIBLE
                }
            } else {
                topLine.visibility = View.INVISIBLE
                bottomLine.visibility = View.INVISIBLE
                statusCircle.setImageResource(R.drawable.orange_circle)
            }

            statusText.text = serviceStatus.orderStatus
            val timestampDate : Date =  serviceStatus.date.toDate()
            val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.forLanguageTag("pl_PL")).format(timestampDate)
            statusTime.text = date.toString()
        }

    }
}

