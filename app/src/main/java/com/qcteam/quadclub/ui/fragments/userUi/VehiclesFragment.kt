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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.qcteam.quadclub.data.helpers.AppHelper
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Quad
import com.qcteam.quadclub.data.adapters.VehicleAdapter
import com.qcteam.quadclub.data.enums.FragmentEnums
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentVehiclesBinding


class VehiclesFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentVehiclesBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //Variable holding reference to dialog that appears during updating data in Firebase
    private var uploadingDataDialog: AlertDialog? = null

    //Variables responsible for bottom add new vehicle sheet dialog
    private lateinit var addNewVehicleDialog: BottomSheetDialog
    private lateinit var addNewVehicleBottomSheetView: View
    private lateinit var addNewVehiclePhotoCardButton: CardView
    private lateinit var addNewVehicleName: TextInputLayout
    private lateinit var addNewVehicleModel: TextInputLayout
    private lateinit var addNewVehicleVinNumber: TextInputLayout
    private lateinit var addNewVehicleMileage: TextInputLayout
    private lateinit var addNewVehicleManufacturer: TextInputLayout
    private lateinit var addNewVehicleEngineCapacity: TextInputLayout
    private lateinit var addNewVehicleColor: TextInputLayout
    private lateinit var addNewVehicleSaveDataButton: Button
    private lateinit var addNewVehiclePhoto: ImageView
    private lateinit var addNewVehiclePhotoErrorText: TextView
    private lateinit var addNewVehiclePhotoPlusButton: ImageView
    private lateinit var addNewVehicleCloseDialogButton: ImageButton

    //Variable holding bitmap of selected vehicle photo
    private var selectedBitmap: Bitmap? = null

    //Activity Result Launcher - when user select new profile photo it places new photo in editUserDataUserPhoto ImageView and update selectedBitmap variable
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                addNewVehiclePhotoPlusButton.visibility = View.INVISIBLE
                val source = ImageDecoder.createSource(this.requireContext().contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                //Resizing selected photo ----------------
                val resizedBitmap = AppHelper.resizePhoto(500, bitmap)
                //----------------------------------------

                selectedBitmap = resizedBitmap
                addNewVehiclePhoto.setImageBitmap(resizedBitmap)
            }
        }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentVehiclesBinding.inflate(layoutInflater, container, false)

        //Inflate add new vehicle bottom sheet view
        addNewVehicleBottomSheetView = layoutInflater.inflate(R.layout.add_vehicle_bottom_sheet, container, false)
        //Setup variables holding android widgets references from addNewVehicleBottomSheetView
        setupAddNewVehicleBottomSheet()

        //Setting up listeners for errors
        setupErrorListener()

        //Handling back press buttons actions
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()?.navigate(R.id.action_vehiclesFragment_to_userHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        binding.backButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_vehiclesFragment_to_userHomeFragment)
        }

        //Setting up recycler view containing user vehicles
        setupVehicleListRecyclerView()

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                if (uploadingDataDialog != null && uploadingDataDialog?.isShowing == true) {
                    uploadingDataDialog?.cancel()
                }
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }


    private fun setupAddNewVehicleBottomSheet() {
        addNewVehiclePhotoCardButton = addNewVehicleBottomSheetView.findViewById(R.id.upload_photo_card)
        addNewVehicleName = addNewVehicleBottomSheetView.findViewById(R.id.add_vehicle_name)
        addNewVehicleModel = addNewVehicleBottomSheetView.findViewById(R.id.add_vehicle_model)
        addNewVehicleVinNumber = addNewVehicleBottomSheetView.findViewById(R.id.add_vehicle_vin)
        addNewVehicleMileage = addNewVehicleBottomSheetView.findViewById(R.id.add_vehicle_mileage)
        addNewVehicleManufacturer = addNewVehicleBottomSheetView.findViewById(R.id.add_vehicle_manufacturer)
        addNewVehicleEngineCapacity = addNewVehicleBottomSheetView.findViewById(R.id.add_vehicle_engine_capacity)
        addNewVehicleColor = addNewVehicleBottomSheetView.findViewById(R.id.add_vehicle_color)
        addNewVehicleSaveDataButton = addNewVehicleBottomSheetView.findViewById(R.id.add_new_vehicle_btn)
        addNewVehiclePhoto = addNewVehicleBottomSheetView.findViewById(R.id.add_vehicle_photo)
        addNewVehiclePhotoErrorText = addNewVehicleBottomSheetView.findViewById(R.id.vehicle_photo_error)
        addNewVehiclePhotoPlusButton = addNewVehicleBottomSheetView.findViewById(R.id.upload_vehicle_photo_plus)
        addNewVehicleCloseDialogButton = addNewVehicleBottomSheetView.findViewById(R.id.close_dialog)

        addNewVehicleDialog = BottomSheetDialog(this.requireContext())
        addNewVehicleDialog.setContentView(addNewVehicleBottomSheetView)
        addNewVehicleDialog.setCancelable(false)
        addNewVehicleDialog.behavior.apply {
            isFitToContents = true
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = false
        }

        binding.addVehicleButton.setOnClickListener {
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                setupBottomAddNewVehicleDialogFunctionality()
                setupUploadingNewVehicleDataDialog()
                addNewVehicleDialog.show()
                addNewVehicleDialog.setOnCancelListener {
                    addNewVehicleDialog.cancel()
                    clearBottomAddNewVehicleDialogErrors()
                }
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }
    }


    private fun setupBottomAddNewVehicleDialogFunctionality() {

        addNewVehiclePhotoCardButton.setOnClickListener {
            addPhotoFromGallery()
        }

        addNewVehicleCloseDialogButton.setOnClickListener {
            addNewVehicleDialog.cancel()
        }

        addNewVehicleSaveDataButton.setOnClickListener {

            var validate = 0
            validate += checkVehiclePhoto(selectedBitmap)
            validate += checkVehicleName()
            validate += checkVehicleModel()
            validate += checkVehicleManufacturer()
            validate += checkVehicleEngineCapacity()
            validate += checkVehicleColor()
            validate += checkVehicleMileage()
            validate += checkVehicleVinNumber()


            if (validate == 8) {
                val vehicle = Quad(
                    addNewVehicleName.editText?.text?.trim().toString(),
                    addNewVehicleModel.editText?.text?.trim().toString(),
                    addNewVehicleVinNumber.editText?.text?.trim().toString(),
                    addNewVehicleMileage.editText?.text?.trim().toString().toFloat(),
                    "", "",
                    addNewVehicleManufacturer.editText?.text?.trim().toString(),
                    addNewVehicleEngineCapacity.editText?.text?.trim().toString().toFloat(),
                    addNewVehicleColor.editText?.text?.trim().toString()
                )

                uploadingDataDialog?.show()

                firebaseRepository.uploadNewVehicle(vehicle, selectedBitmap!!)
            }
        }

        firebaseRepository.getTaskStatusListenerData().observe(viewLifecycleOwner, { result ->
            if (result != null) {
                if (result) {
                    uploadingDataDialog?.cancel()
                    addNewVehicleDialog.dismiss()
                    firebaseRepository.resetTaskListeners()
                }
            }
        })
    }

    private fun clearBottomAddNewVehicleDialogErrors() {
        addNewVehicleName.error = null
        addNewVehicleModel.error = null
        addNewVehicleVinNumber.error = null
        addNewVehicleMileage.error = null
        addNewVehicleManufacturer.error = null
        addNewVehicleEngineCapacity.error = null
        addNewVehicleColor.error = null
        addNewVehiclePhotoErrorText.visibility = View.INVISIBLE
        addNewVehiclePhoto.setImageResource(0)
        addNewVehiclePhotoPlusButton.visibility = View.VISIBLE
        addNewVehicleName.isErrorEnabled = false
        addNewVehicleModel.isErrorEnabled = false
        addNewVehicleVinNumber.isErrorEnabled = false
        addNewVehicleMileage.isErrorEnabled = false
        addNewVehicleManufacturer.isErrorEnabled = false
        addNewVehicleEngineCapacity.isErrorEnabled = false
        addNewVehicleColor.isErrorEnabled = false
    }

    private fun setupVehicleListRecyclerView() {

        val linearLayoutManager = LinearLayoutManager(this.context)
        binding.vehicleListRecyclerView.layoutManager = linearLayoutManager

        firebaseRepository.getUserVehicleListData().observe(viewLifecycleOwner, { quadList ->
            if (quadList != null) {
                val adapter = VehicleAdapter(quadList, FragmentEnums.VEHICLES_LIST_FRAGMENT)
                adapter.notifyDataSetChanged()
                binding.vehicleListRecyclerView.adapter = adapter

                binding.vehicleListRecyclerView.visibility = View.VISIBLE
                binding.vehicleListRecyclerViewEmpty.visibility = View.GONE
            } else {
                binding.vehicleListRecyclerView.visibility = View.GONE
                binding.vehicleListRecyclerViewEmpty.visibility = View.VISIBLE
            }
        })
    }

    private fun setupUploadingNewVehicleDataDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_uplading_data, null)
        val alertBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        alertBuilder.setCancelable(false)
        uploadingDataDialog = alertBuilder.create()
    }

    private fun addPhotoFromGallery() {
        getContent.launch("image/*")
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

    //Functions for photo and text fields validation
    private fun checkVehiclePhoto(bitmap: Bitmap?): Int {
        return if (bitmap != null) {
            1
        } else {
            addNewVehiclePhotoErrorText.visibility = View.VISIBLE
            addNewVehiclePhotoCardButton.setOnClickListener {
                addNewVehiclePhotoErrorText.visibility = View.INVISIBLE
                addPhotoFromGallery()
            }
            0
        }
    }

    private fun checkVehicleName(): Int {
        val name = addNewVehicleName.editText?.text?.trim().toString()
        return if (name.isNotEmpty()) {
            addNewVehicleName.error = null
            1
        } else {
            addNewVehicleName.error = requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleModel(): Int {
        val model = addNewVehicleModel.editText?.text?.trim().toString()
        return if (model.isNotEmpty()) {
            addNewVehicleModel.error = null
            1
        } else {
            addNewVehicleModel.error = requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleManufacturer(): Int {
        val manufacturer = addNewVehicleManufacturer.editText?.text?.trim().toString()
        return if (manufacturer.isNotEmpty()) {
            addNewVehicleManufacturer.error = null
            1
        } else {
            addNewVehicleManufacturer.error = requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleEngineCapacity(): Int {
        val engineCapacity = addNewVehicleEngineCapacity.editText?.text?.trim().toString()
        return if (engineCapacity.isNotEmpty()) {
            if (engineCapacity.toFloat() >= 0 && engineCapacity.toFloat() < 1500) {
                addNewVehicleEngineCapacity.error = null
                1
            } else {
                addNewVehicleEngineCapacity.error = requireContext().resources.getString(R.string.str_enter_proper_engine_capacity)
                0
            }
        } else {
            addNewVehicleEngineCapacity.error = requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleColor(): Int {
        val color = addNewVehicleColor.editText?.text?.trim().toString()
        return if (color.isNotEmpty()) {
            return if (color.matches("^[a-żA-Ż]*$".toRegex())) {
                addNewVehicleColor.error = null
                1
            } else {
                addNewVehicleColor.error = requireContext().resources.getString(R.string.enter_proper_data_msg)
                0
            }
        } else {
            addNewVehicleColor.error = requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleMileage(): Int {
        val mileage = addNewVehicleMileage.editText?.text?.trim().toString()
        return if (mileage.isNotEmpty()) {
            if (mileage.toFloat() >= 0 && mileage.toFloat() < 500000) {
                addNewVehicleMileage.error = null
                1
            } else {
                addNewVehicleMileage.error = requireContext().resources.getString(R.string.str_enter_proper_mileage)
                0
            }
        } else {
            addNewVehicleMileage.error = requireContext().resources.getString(R.string.str_mandatory_field)
            0
        }
    }

    private fun checkVehicleVinNumber(): Int {
        val vinNumber = addNewVehicleVinNumber.editText?.text?.trim().toString()
        if (vinNumber.isEmpty()) {
            addNewVehicleVinNumber.error = requireContext().resources.getString(R.string.str_mandatory_field)
            return 0
        } else {
            val letterValue = hashMapOf(
                'A' to 1,
                'B' to 2,
                'C' to 3,
                'D' to 4,
                'E' to 5,
                'F' to 6,
                'G' to 7,
                'H' to 8,
                'J' to 1,
                'K' to 2,
                'L' to 3,
                'M' to 4,
                'N' to 5,
                'P' to 7,
                'R' to 9,
                'S' to 2,
                'T' to 3,
                'U' to 4,
                'V' to 5,
                'W' to 6,
                'X' to 7,
                'Y' to 8,
                'Z' to 9,
                '1' to 1,
                '2' to 2,
                '3' to 3,
                '4' to 4,
                '5' to 5,
                '6' to 6,
                '7' to 7,
                '8' to 8,
                '9' to 9,
                '0' to 0
            )

            val positionWeight: IntArray = intArrayOf(
                8,
                7,
                6,
                5,
                4,
                3,
                2,
                10,
                0,
                9,
                8,
                7,
                6,
                5,
                4,
                3,
                2
            )


            if (vinNumber.length == 17) {
                var csc = 0
                for (i in 0..16) {
                    csc += positionWeight[i] * letterValue[vinNumber[i]]!!
                }
                csc %= 11
                if (csc == 10) {
                    return if (vinNumber[8] == 'X') {
                        addNewVehicleVinNumber.error = null
                        1
                    } else {
                        addNewVehicleVinNumber.error = requireContext().resources.getString(R.string.str_enter_proper_vin_number)
                        0
                    }
                } else {
                    return if (letterValue[vinNumber[8]]!! == csc) {
                        addNewVehicleVinNumber.error = null
                        1
                    } else {
                        addNewVehicleVinNumber.error = requireContext().resources.getString(R.string.str_enter_proper_vin_number)
                        0
                    }
                }
            } else {
                addNewVehicleVinNumber.error = requireContext().resources.getString(R.string.str_enter_proper_vin_number)
                return 0
            }
        }

    }
    //-----------------------------------------------

}