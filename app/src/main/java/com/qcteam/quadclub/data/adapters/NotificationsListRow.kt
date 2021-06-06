package com.qcteam.quadclub.data.adapters

import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Notification
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item

class NotificationsListRow(val notification: Notification) : Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.notifications_list_row_message_text).text = notification.message
        viewHolder.itemView.findViewById<TextView>(R.id.notifications_list_row_timestamp_text).text = notification.timestamp

        if(notification.displayed == true){
            viewHolder.itemView.findViewById<TextView>(R.id.notifications_list_row_message_text).setTextColor((ContextCompat.getColor(viewHolder.itemView.context, R.color.light_gray)))
            viewHolder.itemView.findViewById<TextView>(R.id.notifications_list_row_timestamp_text).setTextColor((ContextCompat.getColor(viewHolder.itemView.context, R.color.light_gray)))
        }

        viewHolder.itemView.findViewById<ImageButton>(R.id.notifications_list_row_delete_button).setOnClickListener {
            FirebaseRepository().deleteNotification(notification.notificationKey!!)
        }
    }

    override fun getLayout(): Int {
        return R.layout.notifications_list_row
    }
}