package com.qcteam.quadclub.data.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Route
import com.qcteam.quadclub.data.helpers.PARAM_ROUTE_NAME

class RouteAdapter(private val routeList: List<Route>) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.recycler_route_card, parent, false)

        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val item = routeList[position]
        holder.bindRoute(item)
    }

    override fun getItemCount(): Int {
        return routeList.size
    }

    class RouteViewHolder(v: View) : RecyclerView.ViewHolder(v),
        View.OnClickListener {

        private var view: View = v
        private lateinit var route: Route

        private var name: TextView = view.findViewById(R.id.recycler_route_card_name)
        private var date: TextView = view.findViewById(R.id.recycler_route_card_date)
        private var distance: TextView = view.findViewById(R.id.recycler_route_card_distance)

        init {
            view.setOnClickListener(this)
        }


        override fun onClick(v: View) {
            val bundle = bundleOf(
                PARAM_ROUTE_NAME to route.name!!
            )
            view.findNavController().navigate(R.id.action_routesFragment_to_routeMoreInfoFragment, bundle)
        }

        fun bindRoute(item: Route) {
            this.route = item

            name.text = route.name
            date.text = route.date
            distance.text = String.format("%.2f km", route.distance)
        }

    }

}