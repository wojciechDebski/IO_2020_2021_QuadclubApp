package com.qcteam.quadclub.ui.fragments.userUi

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.qcteam.quadclub.data.helpers.AppHelper
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Quad
import com.qcteam.quadclub.data.adapters.OrdersAdapterInVehicle
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_VIN_NUMBER
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentVehicleMoreInfoBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.properties.Delegates



class VehicleMoreInfoFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentVehicleMoreInfoBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //Boolean variable holding: true - when specific card is open or false - otherwise
    private var vehicleMoreInfoCardOpen: Boolean = false
    private var serviceMoreInfoCardOpen: Boolean = false

    //Variable holding parameter passed by navigation bundle
    private var vehicleVinNumberParameter: String? = null

    //Variable holding information about actually showing vehicle
    private var showingVehicle: Quad? = null

    //Variable holding reference to dialog that appears during updating data in Firebase
    private var updatingDataDialog: AlertDialog? = null

    //Variables responsible for bottom edit vehicle info sheet dialog
    private var editVehicleDialog: BottomSheetDialog? = null
    private lateinit var editVehicleBottomSheetView: View
    private lateinit var editVehiclePhotoCardButton: CardView
    private lateinit var editVehicleName: TextInputLayout
    private lateinit var editVehicleModel: TextInputLayout
    private lateinit var editVehicleVin: TextInputLayout
    private lateinit var editVehicleMileage: TextInputLayout
    private lateinit var editVehicleManufacturer: TextInputLayout
    private lateinit var editVehicleEngineCapacity: TextInputLayout
    private lateinit var editVehicleColor: TextInputLayout
    private lateinit var editVehicleUpdateDataButton: Button
    private lateinit var editVehiclePhoto: ImageView
    private lateinit var editVehiclePhotoErrorText: TextView
    private lateinit var editVehiclePhotoPlusButton: ImageView
    private lateinit var editVehicleCloseDialogButton: ImageButton

    //Variable holding bitmap of selected vehicle photo
    private var selectedBitmap: Bitmap? by Delegates.observable(null) { _, _, newValue ->
        editVehicleUpdateDataButton.isEnabled = newValue != null
    }

    //Activity Result Launcher - when user select new profile photo it places new photo in editUserDataUserPhoto ImageView and update selectedBitmap variable
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                editVehiclePhotoPlusButton.visibility = View.INVISIBLE
                val source = ImageDecoder.createSource(this.requireContext().contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                //Resizing selected photo ----------------
                val resizedBitmap = AppHelper.resizePhoto(500, bitmap)
                //----------------------------------------

                selectedBitmap = resizedBitmap
                editVehiclePhoto.setImageBitmap(resizedBitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            vehicleVinNumberParameter = it.getString(PARAM_VIN_NUMBER)
        }
    }

    @DelicateCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentVehicleMoreInfoBinding.inflate(layoutInflater, container, false)

        //Setting up listeners for errors, updating vehicle status, deletion vehicle status
        setupErrorListener()

        //Setting up chosen vehicle info in proper fields
        getShowingVehicleInfoAndSetupTextFields()

        //Inflate edit vehicle info bottom sheet view
        editVehicleBottomSheetView =
            layoutInflater.inflate(R.layout.add_vehicle_bottom_sheet, container, false)
        //Setup variables holding android widgets references from editVehicleBottomSheetView
        setupEditVehicleBottomSheet()

        //Handling back press buttons actions
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_vehicleMoreInfoFragment_to_vehiclesFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()
                ?.navigate(R.id.action_vehicleMoreInfoFragment_to_vehiclesFragment)
        }

        //Handling delete vehicle button pressed
        binding.vehicleInfoDeleteButton.setOnClickListener {
            deleteVehicleAction()
        }
        //-------------------------------------

        //Setting up vehicle more info card functionality
        setupVehicleMoreInfoCardFunctionality()

        //Setting up vehicle service orders history card functionality
        setupVehicleServiceOrdersMoreInfoCardFunctionality()

        return binding.root
    }

    //listeners ---------------------------------------------
    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                if (updatingDataDialog != null && updatingDataDialog?.isShowing == true) {
                    updatingDataDialog?.cancel()
                }
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    //-------------------------------------------------------

    private fun getShowingVehicleInfoAndSetupTextFields() {
        firebaseRepository.getUserVehicleListData().observe(viewLifecycleOwner, { vehicleList ->
            if (vehicleList != null) {
                showingVehicle = vehicleList.find {
                    it.vehicleVinNumber == vehicleVinNumberParameter
                }

                if (showingVehicle != null) {
                    binding.vehicleInfoName.text = showingVehicle!!.vehicleName
                    binding.vehicleInfoModel.text = showingVehicle!!.vehicleModel
                    binding.vehicleInfoMileage.text =
                        showingVehicle!!.vehicleCurrentMileage_toString()
                    binding.vehicleInfoManufacturer.text = showingVehicle!!.vehicleManufacturer
                    binding.vehicleInfoEngineCapacity.text =
                        showingVehicle!!.vehicleEngineCapacity_toString()
                    binding.vehicleInfoColor.text = showingVehicle!!.vehicleColor

                    Glide.with(requireView()).load(showingVehicle!!.vehiclePhotoUrl).centerCrop()
                        .diskCacheStrategy(
                            DiskCacheStrategy.RESOURCE
                        ).into(binding.vehicleInfoPhoto)
                }
            }
        })
    }

    //Edit vehicle bottom sheet -----------------------------
    private fun setupEditVehicleBottomSheet() {
        editVehiclePhotoCardButton = editVehicleBottomSheetView.findViewById(R.id.upload_photo_card)
        editVehicleName = editVehicleBottomSheetView.findViewById(R.id.add_vehicle_name)
        editVehicleModel = editVehicleBottomSheetView.findViewById(R.id.add_vehicle_model)
        editVehicleVin = editVehicleBottomSheetView.findViewById(R.id.add_vehicle_vin)
        editVehicleMileage = editVehicleBottomSheetView.findViewById(R.id.add_vehicle_mileage)
        editVehicleManufacturer =
            editVehicleBottomSheetView.findViewById(R.id.add_vehicle_manufacturer)
        editVehicleEngineCapacity =
            editVehicleBottomSheetView.findViewById(R.id.add_vehicle_engine_capacity)
        editVehicleColor = editVehicleBottomSheetView.findViewById(R.id.add_vehicle_color)
        editVehicleUpdateDataButton =
            editVehicleBottomSheetView.findViewById(R.id.add_new_vehicle_btn)
        editVehiclePhoto = editVehicleBottomSheetView.findViewById(R.id.add_vehicle_photo)
        editVehiclePhotoErrorText =
            editVehicleBottomSheetView.findViewById(R.id.vehicle_photo_error)
        editVehiclePhotoPlusButton =
            editVehicleBottomSheetView.findViewById(R.id.upload_vehicle_photo_plus)
        editVehiclePhotoPlusButton.visibility = View.GONE
        editVehicleCloseDialogButton = editVehicleBottomSheetView.findViewById(R.id.close_dialog)

        editVehicleDialog = BottomSheetDialog(this.requireContext())
        if (editVehicleDialog != null) {
            editVehicleDialog!!.setContentView(editVehicleBottomSheetView)
            editVehicleDialog!!.setCancelable(false)
            editVehicleDialog!!.behavior.apply {
                isFitToContents = true
                state = BottomSheetBehavior.STATE_EXPANDED
                isDraggable = false
            }
        }


        binding.vehicleInfoEditButton.setOnClickListener {
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                setupEditVehicleBottomSheetPhotoAndTextFields()
                setupEditVehicleBottomSheetFunctionality()

                if (editVehicleDialog != null) {
                    editVehicleDialog!!.show()
                    editVehicleDialog!!.setOnCancelListener {
                        editVehicleDialog!!.cancel()
                        clearBottomEditVehicleDialogErrors()
                    }
                }
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }
    }


    private fun setupEditVehicleBottomSheetFunctionality() {
        setupUpdatingVehicleInfoDialog()

        editVehicleUpdateDataButton.isEnabled = false

        editVehiclePhotoCardButton.setOnClickListener {
            addPhotoFromGallery()
        }

        editVehicleCloseDialogButton.setOnClickListener {
            editVehicleDialog!!.cancel()
        }

        editVehicleName.editText?.addTextChangedListener {
            editVehicleUpdateDataButton.isEnabled = it.toString() != showingVehicle!!.vehicleName
        }
        editVehicleModel.editText?.addTextChangedListener {
            editVehicleUpdateDataButton.isEnabled = it.toString() != showingVehicle!!.vehicleModel
        }
        editVehicleMileage.editText?.addTextChangedListener {
            editVehicleUpdateDataButton.isEnabled =
                it.toString() != showingVehicle!!.vehicleCurrentMileage.toString()
        }
        editVehicleManufacturer.editText?.addTextChangedListener {
            editVehicleUpdateDataButton.isEnabled =
                it.toString() != showingVehicle!!.vehicleManufacturer
        }
        editVehicleEngineCapacity.editText?.addTextChangedListener {
            editVehicleUpdateDataButton.isEnabled =
                it.toString() != showingVehicle!!.vehicleEngineCapacity.toString()
        }
        editVehicleColor.editText?.addTextChangedListener {
            editVehicleUpdateDataButton.isEnabled = it.toString() != showingVehicle!!.vehicleColor
        }

        editVehicleUpdateDataButton.setOnClickListener {
            val update = mutableMapOf<String, Any>()

            if (editVehicleName.editText?.text.toString() != showingVehicle!!.vehicleName) {
                if (checkVehicleName() == 1) {
                    update["vehicleName"] = editVehicleName.editText?.text.toString()
                }
            }

            if (editVehicleModel.editText?.text.toString() != showingVehicle!!.vehicleModel) {
                if (checkVehicleModel() == 1) {
                    update["vehicleModel"] = editVehicleModel.editText?.text.toString()
                }
            }

            if (editVehicleMileage.editText?.text.toString() != showingVehicle!!.vehicleCurrentMileage.toString()) {
                if (checkVehicleMileage() == 1) {
                    update["vehicleCurrentMileage"] =
                        editVehicleMileage.editText?.text.toString().toFloat()
                }
            }

            if (editVehicleManufacturer.editText?.text.toString() != showingVehicle!!.vehicleManufacturer) {
                if (checkVehicleManufacturer() == 1) {
                    update["vehicleManufacturer"] =
                        editVehicleManufacturer.editText?.text.toString()
                }
            }

            if (editVehicleEngineCapacity.editText?.text.toString() != showingVehicle!!.vehicleEngineCapacity.toString()) {
                if (checkVehicleEngineCapacity() == 1) {
                    update["vehicleEngineCapacity"] =
                        editVehicleEngineCapacity.editText?.text.toString().toFloat()
                }
            }

            if (editVehicleColor.editText?.text.toString() != showingVehicle!!.vehicleColor.toString()) {
                if (checkVehicleColor() == 1) {
                    update["vehicleColor"] = editVehicleColor.editText?.text.toString()
                }
            }

            if (update.isNotEmpty() || selectedBitmap != null) {
                updatingDataDialog!!.show()

                firebaseRepository.updateVehicle(update, selectedBitmap, showingVehicle!!)

                firebaseRepository.getTaskStatusListenerData()
                    .observe(viewLifecycleOwner, { status ->
                        if (status != null) {
                            if (status && updatingDataDialog != null && editVehicleDialog != null) {
                                if (updatingDataDialog!!.isShowing) {
                                    updatingDataDialog!!.cancel()
                                    editVehicleDialog!!.cancel()
                                }
                            }
                        }
                    })

            }

        }

    }

    private fun setupEditVehicleBottomSheetPhotoAndTextFields() {
        if (showingVehicle != null) {
            Glide.with(requireView()).load(showingVehicle!!.vehiclePhotoUrl).centerCrop()
                .diskCacheStrategy(
                    DiskCacheStrategy.RESOURCE
                ).into(editVehiclePhoto)

            editVehicleName.editText?.setText(showingVehicle!!.vehicleName)
            editVehicleModel.editText?.setText(showingVehicle!!.vehicleModel)
            editVehicleVin.editText?.setText(showingVehicle!!.vehicleVinNumber)
            editVehicleVin.isEnabled = false
            editVehicleMileage.editText?.setText(showingVehicle!!.vehicleCurrentMileage.toString())
            editVehicleManufacturer.editText?.setText(showingVehicle!!.vehicleManufacturer)
            editVehicleEngineCapacity.editText?.setText(showingVehicle!!.vehicleEngineCapacity.toString())
            editVehicleColor.editText?.setText(showingVehicle!!.vehicleColor)
            editVehicleUpdateDataButton.text =
                requireContext().resources.getString(R.string.str_update_data)
        }
    }

    private fun clearBottomEditVehicleDialogErrors() {
        editVehicleName.error = null
        editVehicleModel.error = null
        editVehicleVin.error = null
        editVehicleMileage.error = null
        editVehicleManufacturer.error = null
        editVehicleEngineCapacity.error = null
        editVehicleColor.error = null
        editVehiclePhotoErrorText.visibility = View.INVISIBLE
        editVehiclePhoto.setImageResource(0)
        editVehiclePhotoPlusButton.visibility = View.VISIBLE
        editVehicleName.isErrorEnabled = false
        editVehicleModel.isErrorEnabled = false
        editVehicleVin.isErrorEnabled = false
        editVehicleMileage.isErrorEnabled = false
        editVehicleManufacturer.isErrorEnabled = false
        editVehicleEngineCapacity.isErrorEnabled = false
        editVehicleColor.isErrorEnabled = false
    }

    private fun addPhotoFromGallery() {
        getContent.launch("image/*")
    }
    //--------------------------------------------------------

    private fun deleteVehicleAction() {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val currentNetwork = connectivityManager.activeNetwork

        if (currentNetwork != null) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(requireContext().resources.getString(R.string.caution_alert_title))
            builder.setMessage(requireContext().resources.getString(R.string.caution_alert_delete_vehicle))
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                firebaseRepository.deleteVehicle(showingVehicle!!.vehicleVinNumber)

                firebaseRepository.getTaskStatusListenerData()
                    .observe(viewLifecycleOwner, { status ->
                        if (status != null) {
                            if (status) {
                                view?.findNavController()?.navigateUp()
                            }
                        }
                    })

            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            builder.show()
        } else {
            AlertBuilder.buildNetworkAlert(requireContext())
        }
    }

    private fun setupVehicleMoreInfoCardFunctionality() {
        binding.vehicleMoreInfoLayoutHeader.setOnClickListener {
            if (vehicleMoreInfoCardOpen) {
                binding.vehicleMoreInfoLayout.visibility = View.GONE
                binding.vehicleMoreInfoLayoutButton.setImageResource(R.drawable.ic_arrow_down)
                binding.vehicleMoreInfoLayoutHeader.setBackgroundResource(0)
                vehicleMoreInfoCardOpen = false
            } else {
                binding.vehicleMoreInfoLayout.visibility = View.VISIBLE
                binding.vehicleMoreInfoLayoutButton.setImageResource(R.drawable.ic_arrow_up)
                binding.vehicleMoreInfoLayoutHeader.setBackgroundResource(R.drawable.recycler_services_list_background)
                vehicleMoreInfoCardOpen = true
            }
        }

        binding.vehicleMoreInfoLayoutButton.setOnClickListener {
            if (vehicleMoreInfoCardOpen) {
                binding.vehicleMoreInfoLayout.visibility = View.GONE
                binding.vehicleMoreInfoLayoutButton.setImageResource(R.drawable.ic_arrow_down)
                binding.vehicleMoreInfoLayoutHeader.setBackgroundResource(0)
                vehicleMoreInfoCardOpen = false
            } else {
                binding.vehicleMoreInfoLayout.visibility = View.VISIBLE
                binding.vehicleMoreInfoLayoutButton.setImageResource(R.drawable.ic_arrow_up)
                binding.vehicleMoreInfoLayoutHeader.setBackgroundResource(R.drawable.recycler_services_list_background)
                vehicleMoreInfoCardOpen = true
            }
        }
    }

    private fun setupVehicleServiceOrdersMoreInfoCardFunctionality() {
        setupVehicleServiceOrdersRecyclerView()

        binding.serviceOrdersHistoryLayoutHeader.setOnClickListener {
            if (serviceMoreInfoCardOpen) {
                binding.serviceOrdersHistoryLayout.visibility = View.GONE
                binding.serviceOrdersHistoryLayoutButton.setImageResource(R.drawable.ic_arrow_down)
                binding.serviceOrdersHistoryLayoutHeader.setBackgroundResource(0)
                serviceMoreInfoCardOpen = false
            } else {
                binding.serviceOrdersHistoryLayout.visibility = View.VISIBLE
                binding.serviceOrdersHistoryLayoutButton.setImageResource(R.drawable.ic_arrow_up)
                binding.serviceOrdersHistoryLayoutHeader.setBackgroundResource(R.drawable.recycler_services_list_background)
                serviceMoreInfoCardOpen = true
            }
        }

        binding.serviceOrdersHistoryLayoutButton.setOnClickListener {
            if (serviceMoreInfoCardOpen) {
                binding.serviceOrdersHistoryLayout.visibility = View.GONE
                binding.serviceOrdersHistoryLayoutButton.setImageResource(R.drawable.ic_arrow_down)
                binding.serviceOrdersHistoryLayoutHeader.setBackgroundResource(0)
                serviceMoreInfoCardOpen = false
            } else {
                binding.serviceOrdersHistoryLayout.visibility = View.VISIBLE
                binding.serviceOrdersHistoryLayoutButton.setImageResource(R.drawable.ic_arrow_up)
                binding.serviceOrdersHistoryLayoutHeader.setBackgroundResource(R.drawable.recycler_services_list_background)
                serviceMoreInfoCardOpen = true
            }
        }
    }

    private fun setupVehicleServiceOrdersRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(this.context)
        binding.vehicleInfoServiceOrdersRecyclerView.layoutManager = linearLayoutManager

        firebaseRepository.getUserServiceOrdersListData().observe(viewLifecycleOwner, { services ->
            if (services != null) {

                val listForVehicle = services.filter { order ->
                    order.vehicle?.vehicleVinNumber == showingVehicle!!.vehicleVinNumber
                }
                if (listForVehicle.isNotEmpty()) {
                    binding.serviceEmptyHistory.visibility = View.GONE
                    val adapter = OrdersAdapterInVehicle(listForVehicle)
                    adapter.notifyDataSetChanged()
                    binding.vehicleInfoServiceOrdersRecyclerView.adapter = adapter
                } else {
                    binding.serviceEmptyHistory.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupUpdatingVehicleInfoDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_uplading_data, null)
        val alertBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        alertBuilder.setCancelable(false)
        updatingDataDialog = alertBuilder.create()
    }


    //Validation fields functions ---------------------
    private fun checkVehicleName(): Int {
        val name = editVehicleName.editText?.text?.trim().toString()
        return if (name.isNotEmpty()) {
            editVehicleName.error = null
            1
        } else {
            editVehicleName.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleModel(): Int {
        val model = editVehicleModel.editText?.text?.trim().toString()
        return if (model.isNotEmpty()) {
            editVehicleModel.error = null
            1
        } else {
            editVehicleModel.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleManufacturer(): Int {
        val manufacturer = editVehicleManufacturer.editText?.text?.trim().toString()
        return if (manufacturer.isNotEmpty()) {
            editVehicleManufacturer.error = null
            1
        } else {
            editVehicleManufacturer.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleEngineCapacity(): Int {
        val engineCapacity = editVehicleEngineCapacity.editText?.text?.trim().toString()
        return if (engineCapacity.isNotEmpty()) {
            if (engineCapacity.toFloat() >= 0 && engineCapacity.toFloat() < 1500) {
                editVehicleEngineCapacity.error = null
                1
            } else {
                editVehicleEngineCapacity.error =
                    requireContext().resources.getString(R.string.str_enter_proper_engine_capacity)
                0
            }
        } else {
            editVehicleEngineCapacity.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleColor(): Int {
        val color = editVehicleColor.editText?.text?.trim().toString()
        return if (color.isNotEmpty()) {
            return if (color.matches("^[a-żA-Ż]*$".toRegex())) {
                editVehicleColor.error = null
                1
            } else {
                editVehicleColor.error = requireContext().resources.getString(R.string.enter_proper_data_msg)
                0
            }
        } else {
            editVehicleColor.error = requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleMileage(): Int {
        val mileage = editVehicleMileage.editText?.text?.trim().toString()
        return if (mileage.isNotEmpty()) {
            if (mileage.toFloat() >= 0 && mileage.toFloat() < 500000) {
                editVehicleMileage.error = null
                1
            } else {
                editVehicleMileage.error =
                    requireContext().resources.getString(R.string.str_enter_proper_mileage)
                0
            }
        } else {
            editVehicleMileage.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }
    //--------------------------------------------------

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}