package com.qcteam.quadclub.data.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.qcteam.quadclub.R


class StatusSpinnerAdapter(val context: Context, private val statusList: List<String>) :
    BaseAdapter() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return statusList.size
    }

    override fun getItem(position: Int): Any {
        return statusList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val vh: ItemRowHolder
        if (convertView == null) {
            view = mInflater.inflate(R.layout.spinner_status_adapter, parent, false)
            vh = ItemRowHolder(view)
            view?.tag = vh
        } else {
            view = convertView
            vh = view.tag as ItemRowHolder
        }

        vh.status.text = statusList[position]
        return view
    }

    private class ItemRowHolder(row: View) {
        val status: TextView = row.findViewById(R.id.spinner_order_status)
    }
}