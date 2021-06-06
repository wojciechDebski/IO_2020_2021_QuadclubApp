package com.qcteam.quadclub.ui.fragments.userUi

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.qcteam.quadclub.ForegroundTrackingService
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Route
import com.qcteam.quadclub.data.adapters.QuadSpinnerAdapter
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.TRACKING_PERMISSIONS_REQUEST_CODE
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentTrackingBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class TrackingFragment : Fragment(), OnMapReadyCallback {

    //binding view variable holder
    private lateinit var binding: FragmentTrackingBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //Variable holding reference to dialog that appears during uploading route data in Firebase
    private var uploadingDataDialog: AlertDialog? = null

    //variables responsible for tracking
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var map: GoogleMap
    private lateinit var gpsTrack: Polyline

    private var distance: Float = 0.00F

    // Number of seconds displayed
    // on the stopwatch.
    private var seconds = 0

    // Is the stopwatch running?
    private var running = false
    private var firstRun = false
    private var wasRunning = false
    private var returnTime: String = ""


    //save route widgets variables --------------
    private var saveRouteDialog: AlertDialog? = null
    private lateinit var saveRouteName: EditText
    private lateinit var saveRouteVehiclesSpinner: Spinner
    private lateinit var saveRouteRadioGroup: RadioGroup
    private lateinit var saveRouteSaveButton: Button
    private lateinit var saveRouteCloseButton: ImageButton

    //--------------------------------------------


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentTrackingBinding.inflate(layoutInflater, container, false)
        binding.trackingStartButtonStartingPoint.isEnabled = false

        //Handling back press buttons actions
        binding.trackingCloseButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_trackingFragment_to_userHomeFragment)
        }
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                activity?.moveTaskToBack(true)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        //setting ut error listener
        setupErrorListener()

        //setting up location permission checker
        setupFirstCheckPermissions()

        if (savedInstanceState != null) {
            // Get the previous state of the stopwatch
            // if the activity has been
            // destroyed and recreated.
            seconds = savedInstanceState
                .getInt("seconds")
            running = savedInstanceState
                .getBoolean("running")
            wasRunning = savedInstanceState
                .getBoolean("wasRunning")
        }

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(requireContext().resources.getString(R.string.caution_alert_title))
                builder.setMessage(error.localizedMessage)

                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    if (gpsTrack.points.isNotEmpty()) {
                        map.clear()
                    }
                    if (saveRouteDialog != null && saveRouteDialog!!.isShowing) {
                        saveRouteDialog!!.dismiss()
                    }
                }

                builder.show()
            }
        })
    }

    private fun setupFirstCheckPermissions() {
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())


        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                TRACKING_PERMISSIONS_REQUEST_CODE
            )
        } else {
            val locationManager =
                activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                val mapFragment =
                    childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                mapFragment?.getMapAsync(this)
                permissionsGrantedTracking()
                getLocationUpdates()
            } else {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(requireContext().resources.getString(R.string.caution_alert_title))
                builder.setMessage(requireContext().resources.getString(R.string.empty_vehicle_colection_appoitment))
                builder.setNeutralButton("Odśwież") { _, _ ->
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        val mapFragment =
                            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                        mapFragment?.getMapAsync(this)
                        permissionsGrantedTracking()
                        getLocationUpdates()
                    } else {
                        builder.show()
                    }
                }
                builder.show()
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    location.latitude,
                                    location.longitude
                                ), 15f
                            )
                        )
                    }
                }
        }


    }

    private fun permissionsGrantedTracking() {
        binding.trackingStartButtonStartingPoint.isEnabled = true

        binding.trackingStartButtonStartingPoint.setOnClickListener {
            val connectivityManager =
                requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                binding.counterDownLayout.visibility = View.VISIBLE

                val timer = object : CountDownTimer(4000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val number = (millisUntilFinished / 1000).toInt()
                        if (number != 0) {
                            binding.counterDownText.text = number.toString()
                        } else {
                            binding.counterDownText.textSize = 45f
                            binding.counterDownText.text =
                                requireContext().resources.getString(R.string.str_go_road)
                        }
                    }

                    override fun onFinish() {
                        binding.counterDownLayout.visibility = View.GONE
                        binding.startingPointLayout.visibility = View.GONE
                        binding.trackingPointLayout.visibility = View.VISIBLE
                        binding.trackingCloseButton.visibility = View.GONE
                        startTracking()
                    }
                }

                timer.start()
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }

        binding.trackingPointLayoutEndButton.setOnClickListener {
            stopTracking()
        }


    }

    private fun startTracking() {
        ForegroundTrackingService.startService(
            requireContext(),
            requireContext().resources.getString(R.string.str_route_tracking)
        )

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        if (!running) {
            if (firstRun) onClickStartX()
            else {
                onClickStartX()
                runTimer()
                firstRun = true
            }
        }

        gpsTrack = map.addPolyline(
            PolylineOptions()
                .clickable(false)
                .color(Color.GREEN)
                .width(15F)
        )
    }

    private fun setupSavingRouteDialog() {
        val dialog = LayoutInflater.from(requireContext())
            .inflate(R.layout.save_route_dialog, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialog)

        saveRouteName = dialog.findViewById(R.id.save_route_name)
        saveRouteVehiclesSpinner = dialog.findViewById(R.id.save_route_vehicle_spinner)
        saveRouteRadioGroup = dialog.findViewById(R.id.save_route_radio_group)
        saveRouteSaveButton = dialog.findViewById(R.id.save_route_button)
        saveRouteCloseButton = dialog.findViewById(R.id.save_route_close_dialog)

        firebaseRepository.getUserVehicleListData().observe(viewLifecycleOwner, { vehicleList ->
            if (vehicleList != null) {
                val spinnerAdapter = QuadSpinnerAdapter(requireContext(), vehicleList)
                spinnerAdapter.notifyDataSetChanged()
                saveRouteVehiclesSpinner.adapter = spinnerAdapter
            }
        })

        saveRouteDialog = dialogBuilder.create()
    }

    private fun stopTracking() {
        ForegroundTrackingService.stopService(requireContext())
        setupSavingRouteDialog()
        setupUploadingDataDialog()
        onClickStopX()

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)

        binding.trackingPointLayoutEndButton.visibility = View.GONE

        saveRouteDialog?.show()

        saveRouteCloseButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(requireContext().resources.getString(R.string.caution_alert_title))
            builder.setMessage(requireContext().resources.getString(R.string.str_do_you_want_save_route))

            builder.setPositiveButton("TAK") { _, _ ->
                if (gpsTrack.points.isNotEmpty()) {
                    map.clear()
                }
                saveRouteDialog?.dismiss()
                this.view?.findNavController()
                    ?.navigateUp()
            }

            builder.setNegativeButton("NIE") { _, _ ->
            }

            builder.show()
        }


        saveRouteRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.save_route_only_update_mileage -> {
                    saveRouteName.visibility = View.GONE
                    saveRouteName.error = null
                    saveRouteSaveButton.isEnabled = true
                }
                R.id.save_route_update_mileage_and_save -> {
                    saveRouteName.visibility = View.VISIBLE
                    saveRouteName.addTextChangedListener { text ->
                        if (text.isNullOrEmpty()) {
                            saveRouteName.error =
                                requireContext().resources.getString(R.string.str_mandatory_field)
                            saveRouteSaveButton.isEnabled = false
                        } else {
                            saveRouteName.error = null
                            saveRouteSaveButton.isEnabled = true
                        }
                    }
                }
            }
        }

        saveRouteSaveButton.setOnClickListener {
            val selectedVehicle =
                firebaseRepository.getUserVehicleListData().value?.get(saveRouteVehiclesSpinner.selectedItemPosition)

            when (saveRouteRadioGroup.checkedRadioButtonId) {
                R.id.save_route_only_update_mileage -> {
                    if (selectedVehicle != null) {
                        firebaseRepository.saveRouteOnlyUpdateMileage(selectedVehicle, distance)
                    }
                }
                R.id.save_route_update_mileage_and_save -> {
                    if (selectedVehicle != null) {
                        firebaseRepository.saveRouteAll(
                            Route(
                                saveRouteName.text.toString(),
                                LocalDateTime.now().format(
                                    DateTimeFormatter.ofPattern("d.M.y H:m")
                                ),
                                distance,
                                binding.trackingPointLayoutTime.text.toString(),
                                selectedVehicle.vehicleVinNumber,
                            ),
                            gpsTrack.points
                        )
                    }
                }
            }

            uploadingDataDialog?.show()
        }

        firebaseRepository.getTaskStatusListenerData().observe(viewLifecycleOwner, { status ->
            if (status != null) {
                if (status) {
                    if (uploadingDataDialog != null && uploadingDataDialog!!.isShowing) {
                        uploadingDataDialog!!.cancel()
                        if (gpsTrack.points.isNotEmpty()) {
                            map.clear()
                        }
                        saveRouteDialog?.dismiss()
                        this.view?.findNavController()
                            ?.navigate(R.id.action_trackingFragment_to_routesFragment)
                    }
                } else {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle(requireContext().resources.getString(R.string.caution_alert_title))
                    builder.setMessage(requireContext().resources.getString(R.string.str_route_name_exist))

                    builder.setPositiveButton("OK") { _, _ ->
                        uploadingDataDialog?.cancel()
                        saveRouteName.error =
                            requireContext().resources.getString(R.string.str_choose_other_name)
                    }

                    builder.show()
                }
            }
        })


    }

    private fun setupUploadingDataDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_uplading_data, null)
        val alertBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        alertBuilder.setCancelable(false)
        uploadingDataDialog = alertBuilder.create()
    }

    private fun getLocationUpdates() {
        locationRequest = LocationRequest.create().apply {
            interval = 100
            fastestInterval = 100
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    val points = gpsTrack.points
                    if (points.size > 0) {
                        val floatArray = FloatArray(1)
                        Location.distanceBetween(
                            gpsTrack.points[gpsTrack.points.size - 1].latitude,
                            gpsTrack.points[gpsTrack.points.size - 1].longitude,
                            currentLocation.latitude,
                            currentLocation.longitude,
                            floatArray
                        )
                        val length = (floatArray[0] / 1000)
                        if (length > 0.001) {
                            points.add(currentLocation)
                            gpsTrack.points = points

                            distance += length
                            binding.trackingPointLayoutDistance.text =
                                String.format("%.2f", distance)
                        }
                    } else {
                        points.add(currentLocation)
                        gpsTrack.points = points
                    }
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                }
            }
        }
    }

    // Save the state of the stopwatch
// if it's about to be destroyed.
    override fun onSaveInstanceState(
        savedInstanceState: Bundle
    ) {
        savedInstanceState
            .putInt("seconds", seconds)
        savedInstanceState
            .putBoolean("running", running)
        savedInstanceState
            .putBoolean("wasRunning", wasRunning)
    }

    // If the activity is paused,
// stop the stopwatch.
    override fun onPause() {
        super.onPause()
        wasRunning = running
        running = true
    }

    // If the activity is resumed,
// start the stopwatch
// again if it was running previously.
    override fun onResume() {
        super.onResume()
        if (wasRunning) {
            running = true
        }
    }

    // Start the stopwatch running
// when the Start button is clicked.
// Below method gets called
// when the Start button is clicked.
    private fun onClickStartX() {
        running = true
    }

    // Stop the stopwatch runningZ
// when the Stop button is clicked.
// Below method gets called
// when the Stop button is clicked.
    private fun onClickStopX() {
        running = false

    }


    // Sets the NUmber of seconds on the timer.
// The runTimer() method uses a Handler
// to increment the seconds and
// update the text view.
    private fun runTimer() {

        // Creates a new Handler
        val handler = Handler()

        // Call the post() method,
        // passing in a new Runnable.
        // The post() method processes
        // code without a delay,
        // so the code in the Runnable
        // will run almost immediately.
        handler.post(object : Runnable {
            override fun run() {
                val hours = seconds / 3600
                val minutes = seconds % 3600 / 60
                val secs = seconds % 60

                // Format the seconds into hours, minutes,
                // and seconds.
                val time: String = java.lang.String
                    .format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d", hours,
                        minutes, secs
                    )


                // Set the text view text.
                //timeView.text = time


                if (!running) binding.trackingPointLayoutTime.text = returnTime
                else {
                    binding.trackingPointLayoutTime.text = time
                    returnTime = time
                }

                // If running is true, increment the
                // seconds variable.
                if (running) {
                    seconds++
                }

                // Post the code again
                // with a delay of 1 second.
                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == TRACKING_PERMISSIONS_REQUEST_CODE) {
            if ((grantResults.isNotEmpty()) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                val locationManager =
                    activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val mapFragment =
                        childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                    mapFragment?.getMapAsync(this)
                    permissionsGrantedTracking()
                    getLocationUpdates()
                }

            } else {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(resources.getString(R.string.caution_alert_title))
                builder.setMessage(resources.getString(R.string.str_tracking_need_access_to_localization))
                builder.setPositiveButton(resources.getString(R.string.str_grant_access)) { _, _ ->
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        TRACKING_PERMISSIONS_REQUEST_CODE
                    )
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                    view?.findNavController()
                        ?.navigateUp()
                }
                builder.show()

            }
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}