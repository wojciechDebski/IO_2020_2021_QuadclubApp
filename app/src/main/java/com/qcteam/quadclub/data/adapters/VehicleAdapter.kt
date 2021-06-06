package com.qcteam.quadclub.data.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Constraints
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Quad
import com.qcteam.quadclub.data.enums.FragmentEnums
import com.qcteam.quadclub.data.helpers.PARAM_VIN_NUMBER

class VehicleAdapter(
    private val quadList: List<Quad>,
    private val fragmentEnums: FragmentEnums
) : RecyclerView.Adapter<VehicleAdapter.QuadViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuadViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.recycler_vehicle_card, parent, false)

        return QuadViewHolder(view, fragmentEnums)
    }

    override fun onBindViewHolder(holder: QuadViewHolder, position: Int) {
        val item = quadList[position]
        holder.bindQuad(item)
    }

    override fun getItemCount(): Int {
        return quadList.size
    }

    class QuadViewHolder(v: View, enums: FragmentEnums) : RecyclerView.ViewHolder(v),
        View.OnClickListener {

        private var view: View = v
        private var fragmentEnums: FragmentEnums = enums
        private lateinit var quad: Quad

        private var name: TextView = view.findViewById(R.id.quad_card_name)
        private var model: TextView = view.findViewById(R.id.quad_card_model)
        private var mileage: TextView = view.findViewById(R.id.quad_card_mileage)
        private var photo: ImageView = view.findViewById(R.id.quad_card_photo)

        private var quadCard: CardView = view.findViewById(R.id.recycled_quad_card_box)
        private var quadCardRightText: View = view.findViewById(R.id.recycler_quad_card_right_text)
        private var quadCardMoreInfoBtn: ImageView =
            view.findViewById(R.id.quad_card_more_info_button)

        init {
            when (fragmentEnums) {
                FragmentEnums.HOME_FRAGMENT -> {
                    quadCard.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    quadCardRightText.layoutParams.width =
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    quadCardMoreInfoBtn.visibility = View.INVISIBLE
                }
                FragmentEnums.VEHICLES_LIST_FRAGMENT -> {
                    quadCard.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    val param = quadCard.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(0, 80, 0, 0)
                    quadCardRightText.layoutParams.width = Constraints.LayoutParams.MATCH_CONSTRAINT
                    quadCardMoreInfoBtn.visibility = View.VISIBLE
                }
            }

            view.setOnClickListener(this)
        }


        override fun onClick(v: View?) {
            val bundle = bundleOf(
                PARAM_VIN_NUMBER to quad.vehicleVinNumber,
            )
            if (fragmentEnums == FragmentEnums.VEHICLES_LIST_FRAGMENT) {
                view.findNavController()
                    .navigate(R.id.action_vehiclesFragment_to_vehicleMoreInfoFragment, bundle)
            } else if (fragmentEnums == FragmentEnums.HOME_FRAGMENT) {
                view.findNavController()
                    .navigate(R.id.action_userHomeFragment_to_vehicleMoreInfoFragment, bundle)
            }
        }

        fun bindQuad(item: Quad) {
            this.quad = item

            Glide.with(view).load(quad.vehiclePhotoUrl).centerCrop().diskCacheStrategy(
                DiskCacheStrategy.RESOURCE
            ).into(photo)


            mileage.text = quad.vehicleCurrentMileage_toString()

            if (fragmentEnums == FragmentEnums.HOME_FRAGMENT) {
                if (quad.vehicleModel?.length!! > 15) {
                    var trimmedModel = quad.vehicleModel
                    if (trimmedModel != null) {
                        trimmedModel = trimmedModel.substring(0, 13) + "..."
                    }
                    model.text = trimmedModel
                } else {
                    model.text = quad.vehicleModel
                }
                if (quad.vehicleName?.length!! > 10) {
                    var trimmedName = quad.vehicleName
                    if (trimmedName != null) {
                        trimmedName = trimmedName.substring(0, 10) + "..."
                    }
                    name.text = trimmedName
                } else {
                    name.text = quad.vehicleName
                }
            } else {
                model.text = quad.vehicleModel
                name.text = quad.vehicleName
            }
        }

    }

}


