package com.qcteam.quadclub.data.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Quad


class QuadSpinnerAdapter(val context: Context, private val quadList: List<Quad>) : BaseAdapter() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return quadList.size
    }

    override fun getItem(position: Int): Any {
        return quadList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val vh: ItemRowHolder
        if (convertView == null) {
            view = mInflater.inflate(R.layout.spinner_quad_adapter, parent, false)
            vh = ItemRowHolder(view)
            view?.tag = vh
        } else {
            view = convertView
            vh = view.tag as ItemRowHolder
        }

        vh.name.text = quadList[position].vehicleName
        vh.model.text = quadList[position].vehicleModel

        return view
    }

    private class ItemRowHolder(row: View) {
        val name: TextView = row.findViewById(R.id.spinner_vehicle_name)
        val model: TextView = row.findViewById(R.id.spinner_vehicle_model)
    }
}