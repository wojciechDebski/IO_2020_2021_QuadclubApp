package com.qcteam.quadclub.ui.fragments.userUi

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.qcteam.quadclub.data.helpers.AppHelper
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.Quad
import com.qcteam.quadclub.data.adapters.QuadSpinnerAdapter
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentAddPhotoPostBinding


class AddPhotoPostFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentAddPhotoPostBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    private var createUploadingDataDialog: AlertDialog? = null

    //variables responsible for tag vehicle bottom sheet dialog
    private lateinit var tagVehicleDialog: AlertDialog
    private lateinit var tagVehicleAlertBuilder: AlertDialog.Builder
    private lateinit var tagVehicleDialogView: View
    private lateinit var tagVehicleDialogSpinner: Spinner
    private lateinit var tagVehicleDialogSaveButton: Button
    private lateinit var tagVehicleDialogCloseDialog: ImageButton

    //variable holding selected vehicle to tag in post
    var selectedVehicle: Quad? = null

    //Variable holding bitmap of selected vehicle photo
    private var selectedBitmap: Bitmap? = null

    //Activity Result Launcher - when user select new profile photo it places new photo in editUserDataUserPhoto ImageView and update selectedBitmap variable
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {

                binding.addPhotoButton.visibility = View.INVISIBLE
                val source = ImageDecoder.createSource(this.requireContext().contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                //Resizing selected photo ----------------
                val resizedBitmap = AppHelper.resizePhoto(2000, bitmap)
                //----------------------------------------

                selectedBitmap = resizedBitmap
                binding.selectedPhoto.setImageBitmap(selectedBitmap)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddPhotoPostBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListener()

        //setting up ui button functionality
        setupUiButtonsFunctionality()

        //setting up tag vehicle dialog
        setupTagVehicleDialog()

        //Handling back press buttons actions
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()?.navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        binding.cancelPhotoPost.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigateUp()
        }

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                if (createUploadingDataDialog != null && createUploadingDataDialog!!.isShowing) {
                    createUploadingDataDialog!!.cancel()
                }
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun setupTagVehicleDialog() {
        tagVehicleDialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.tag_vehicle_dialog, null)
        tagVehicleAlertBuilder = AlertDialog.Builder(requireContext())
            .setView(tagVehicleDialogView)
        tagVehicleDialog = tagVehicleAlertBuilder.create()
        tagVehicleDialogSpinner = tagVehicleDialogView.findViewById(R.id.tag_vehicle_spinner)
        tagVehicleDialogSaveButton = tagVehicleDialogView.findViewById(R.id.tag_vehicle_button)
        tagVehicleDialogCloseDialog =
            tagVehicleDialogView.findViewById(R.id.tag_vehicle_close_dialog)

        firebaseRepository.getUserVehicleListData().observe(viewLifecycleOwner, { vehicleList ->
            if (vehicleList != null) {
                val spinnerAdapter =
                    QuadSpinnerAdapter(requireContext(), vehicleList)
                spinnerAdapter.notifyDataSetChanged()
                tagVehicleDialogSpinner.adapter = spinnerAdapter

                tagVehicleDialogSaveButton.setOnClickListener {
                    selectedVehicle = vehicleList[tagVehicleDialogSpinner.selectedItemPosition]
                    if (selectedVehicle != null) {
                        binding.taggedVehicleInfo.visibility = View.VISIBLE
                        binding.taggedVehicleName.text = selectedVehicle!!.vehicleName
                        binding.taggedVehicleModel.text = selectedVehicle!!.vehicleModel

                        binding.deleteTaggedVehicle.setOnClickListener {
                            binding.taggedVehicleInfo.visibility = View.GONE
                            selectedVehicle = null
                        }
                    }
                    tagVehicleDialog.cancel()
                }
            }
        })

        tagVehicleDialogCloseDialog.setOnClickListener {
            tagVehicleDialog.cancel()
        }

        binding.addVehiclePinToPost.setOnClickListener {
            if (firebaseRepository.getUserVehicleListData().value?.isNotEmpty() == true) {
                tagVehicleDialog.show()
            } else {
                AlertBuilder.buildEmptyVehicleListAlert(requireContext())
            }
        }

        binding.addVehiclePinToPostTitle.setOnClickListener {
            if (firebaseRepository.getUserVehicleListData().value?.isNotEmpty() == true) {
                tagVehicleDialog.show()
            } else {
                AlertBuilder.buildEmptyVehicleListAlert(requireContext())
            }
        }

        binding.uploadPhotoPost.setOnClickListener {
            setupUploadingDataDialog()

            if (selectedBitmap != null) {
                if (selectedBitmap != null) {
                    createUploadingDataDialog?.show()
                    firebaseRepository.uploadPhotoPost(
                        selectedBitmap!!,
                        selectedVehicle,
                        binding.tagsDescriptionText.text.toString()
                    )
                }
            } else {
                AlertBuilder.buildEmptyImageViewAlert(requireContext())
            }
        }

        firebaseRepository.getTaskStatusListenerData().observe(viewLifecycleOwner, { taskStatus ->
            if (taskStatus != null) {
                if (taskStatus) {
                    createUploadingDataDialog?.cancel()
                    firebaseRepository.resetTaskListeners()
                    view?.findNavController()?.navigateUp()
                }
            }
        })
    }

    private fun setupUiButtonsFunctionality() {
        choosePhotoFromGallery()

        binding.addPhotoButton.setOnClickListener {
            choosePhotoFromGallery()
        }

        binding.selectedPhoto.setOnClickListener {
            choosePhotoFromGallery()
        }
    }

    private fun setupUploadingDataDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_uplading_data, null)
        val alertBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        alertBuilder.setCancelable(false)
        createUploadingDataDialog = alertBuilder.create()
    }

    private fun choosePhotoFromGallery() {
        getContent.launch("image/*")
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }
}

