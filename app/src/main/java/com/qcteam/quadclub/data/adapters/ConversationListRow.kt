package com.qcteam.quadclub.data.adapters

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.ConversationFF
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView

class ConversationListRow(val conversation: ConversationFF) : Item<GroupieViewHolder>() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.conversation_last_message_text).text =
            conversation.lastMessage
        viewHolder.itemView.findViewById<TextView>(R.id.conversation_name_surname_text).text =
            conversation.senderNameSurname

        val targetImageView =
            viewHolder.itemView.findViewById<CircleImageView>(R.id.conversation_sender_photo)
        if (!conversation.senderPhotoUrl.isNullOrEmpty()) {
            Picasso.get().load(conversation.senderPhotoUrl).into(targetImageView)
        } else {
            targetImageView.setImageDrawable(
                ContextCompat.getDrawable(
                    viewHolder.itemView.context,
                    R.drawable.ic_account_circle_black
                )
            )
        }
    }

    override fun getLayout(): Int {
        return R.layout.conversation_list_row
    }
}