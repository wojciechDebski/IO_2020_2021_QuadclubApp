package com.qcteam.quadclub.data.adapters

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.UserInfoForConversations
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView

class UsersListRow(val user: UserInfoForConversations) : Item<GroupieViewHolder>() {
    var userUid: String? = null

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.user_list_user_name_surname_text).text =
            user.userNameSurname

        val targetImageView =
            viewHolder.itemView.findViewById<CircleImageView>(R.id.user_list_user_photo)
        if (!user.userProfilePhotoUrl.isNullOrEmpty()) {
            Picasso.get().load(user.userProfilePhotoUrl).into(targetImageView)
        } else {
            targetImageView.setImageDrawable(
                ContextCompat.getDrawable(
                    viewHolder.itemView.context,
                    R.drawable.ic_account_circle_black
                )
            )
        }
        userUid = user.userUid
    }

    override fun getLayout(): Int {
        return R.layout.users_list_row
    }
}