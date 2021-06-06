package com.qcteam.quadclub.ui.fragments.userUi

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Route
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_ROUTE_NAME
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentRouteMoreInfoBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch



class RouteMoreInfoFragment : Fragment(), OnMapReadyCallback {

    //binding view variable holder
    lateinit var binding: FragmentRouteMoreInfoBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //variable holding parameter passed by view bundle
    private var routeNameParameter: String? = null

    //variable holding celected route data class object
    private var selectedRoute: Route? = null

    private lateinit var map: GoogleMap
    private var pointsList = listOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            routeNameParameter = it.getString(PARAM_ROUTE_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRouteMoreInfoBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListener()

        //getting selected route info
        getSelectedRouteInformationAndMapPreviewArrayOfPoints()

        //setting up delete route button functionality
        setupDeleteRouteButtonFunctionality()

        //Handling back press buttons actions
        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_routeMoreInfoFragment_to_routesFragment)
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()?.navigate(R.id.action_routeMoreInfoFragment_to_routesFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, {
            if (it != null) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(resources.getString(R.string.caution_alert_title))
                builder.setMessage(it.localizedMessage)

                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    view?.findNavController()
                        ?.navigate(R.id.action_routeMoreInfoFragment_to_routesFragment)
                }

                builder.show()
            }
        })
    }

    private fun getSelectedRouteInformationAndMapPreviewArrayOfPoints() {
        firebaseRepository.getUserRouteListData().observe(viewLifecycleOwner, { routesList ->
            if(routesList != null && routesList.isNotEmpty()) {
                selectedRoute = routesList.find {
                    it.name == routeNameParameter
                }

                if (selectedRoute != null) {
                    firebaseRepository.getMapPreviewArrayOfPoints(routeNameParameter!!)
                    setDataToTextField()
                }
            }
        })
    }

    private fun setDataToTextField() {
        binding.routeMoreInfoNameHeader.text = selectedRoute!!.name
        binding.routeInfoDate.text = selectedRoute!!.date
        binding.routeInfoDistance.text = String.format("%.2f", selectedRoute!!.distance)
        binding.routeInfoTime.text = selectedRoute!!.time


        firebaseRepository.getMapPreviewPointsListData().observe(viewLifecycleOwner, { points ->
            if (points != null) {
                pointsList = points
                val mapFragment =
                    childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                mapFragment?.getMapAsync(this)
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.clear()

        map.addPolyline(
            PolylineOptions().clickable(false)
                .color(Color.GREEN)
                .width(15F)
                .addAll(pointsList)
        )

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pointsList[0], 13f))
        binding.mapPreviewLoadingProgressBar.visibility = View.GONE
    }

    private fun setupDeleteRouteButtonFunctionality() {

        binding.deleteRouteButton.setOnClickListener {
            val connectivityManager =
                view?.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(requireContext().resources.getString(R.string.caution_alert_title))
                builder.setMessage(requireContext().resources.getString(R.string.caution_alert_delete_route))

                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    if (!selectedRoute?.name.isNullOrEmpty()) {
                        firebaseRepository.deleteRoute(selectedRoute!!.name!!)

                        firebaseRepository.getTaskStatusListenerData().observe(viewLifecycleOwner, { status ->
                            if(status != null) {
                                if(status) {
                                    view?.findNavController()
                                        ?.navigate(R.id.action_routeMoreInfoFragment_to_routesFragment)
                                }
                            }
                        })
                    }
                }

                builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                }

                builder.show()
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}