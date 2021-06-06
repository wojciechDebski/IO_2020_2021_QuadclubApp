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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.UserInfo
import com.qcteam.quadclub.data.helpers.*
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentUserProfileBinding
import kotlin.properties.Delegates


class UserProfileFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentUserProfileBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //Variable holding information about logged user
    private var userInfo: UserInfo? = null

    //Variable holding reference to dialog that appears during updating data in Firebase
    private var uploadingDataDialog: AlertDialog? = null

    //Variables responsible for bottom edit user data sheet dialog
    private lateinit var editUserDataDialog: BottomSheetDialog
    private lateinit var editUserDataBottomSheetView: View
    private lateinit var editUserDataPhotoCardLayout: CardView
    private lateinit var editUserDataUserPhoto: ImageView
    private lateinit var editUserDataUserFirstName: TextInputLayout
    private lateinit var editUserDataUserLastName: TextInputLayout
    private lateinit var editUserDataUserAddress: TextInputLayout
    private lateinit var editUserDataUserPostalCode: TextInputLayout
    private lateinit var editUserDataUserCity: TextInputLayout
    private lateinit var editUserDataUserPhoneNumber: TextInputLayout
    private lateinit var editUserDataSaveChangesButton: Button
    private lateinit var editUserDataCloseDialogButton: ImageButton
    //----------------------------------------------------------------

    //Observable variable holding bitmap of selected user profile photo (null - when user does not selected new profile photo, bitmap - otherwise)
    private var selectedBitmap: Bitmap? by Delegates.observable(null) { _, _, newValue ->
        editUserDataSaveChangesButton.isEnabled = newValue != null
    }

    //Activity Result Launcher - when user select new profile photo it places new photo in editUserDataUserPhoto ImageView and update selectedBitmap variable
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val source = ImageDecoder.createSource(this.requireContext().contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                //Resizing selected photo ----------------
                val resizedBitmap = AppHelper.resizePhoto(500, bitmap)
                //----------------------------------------

                selectedBitmap = resizedBitmap
                editUserDataUserPhoto.setImageBitmap(selectedBitmap)
            }
        }

    //Boolean variable holding: true - when card with user additional info is open or false - otherwise
    private var userMoreInfoCardOpenStatus = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserProfileBinding.inflate(layoutInflater, container, false)

        //Inflate edit user bottom sheet view
        editUserDataBottomSheetView =
            layoutInflater.inflate(R.layout.edit_personal_info_bottom_sheet, container, false)
        //Setup variables holding android widgets references from editUserDataBottomSheetView
        setupEditUserDataBottomSheet()

        setupErrorListener()

        //Handling back press buttons actions
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_userProfileFragment_to_userHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_userProfileFragment_to_userHomeFragment)
        }
        //------------------------------------

        //Handling logout button press
        binding.logoutButton.setOnClickListener {
            firebaseRepository.signOutUser()

            //Observe if user is still logged. When user click "logout" button view is changing to loginFragment
            firebaseRepository.getAuthorizedUserData().observe(viewLifecycleOwner, { user ->
                if (user == null) {
                    firebaseRepository.resetTaskListeners()
                    view?.findNavController()
                        ?.navigate(R.id.action_userProfileFragment_to_loginFragment)
                }
            })
        }

        //Setting up user info data into proper fields
        setupUserInfoDataFields()

        //Setting up user more info card functionality
        setupUserMoreInfoCardFunctionality()

        //Setting up services card functionality
        setupServicesCardFunctionality()

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

    private fun setupUserInfoDataFields() {
        firebaseRepository.getLoggedUserInfoData().observe(viewLifecycleOwner, { _userInfo ->
            if (_userInfo != null) {
                userInfo = _userInfo

                binding.userProfileEmailText.text = userInfo!!.emailAddress
                    ?: requireContext().resources.getString(R.string.str_no_data)
                binding.userProfilePhoneText.text = userInfo!!.phoneNumber
                    ?: requireContext().resources.getString(R.string.str_no_data)
                binding.userProfileAddressText.text =
                    userInfo!!.address ?: requireContext().resources.getString(R.string.str_no_data)
                binding.userProfilePostalCodeText.text = userInfo!!.postalCode
                    ?: requireContext().resources.getString(R.string.str_no_data)
                binding.userProfileCityText.text =
                    userInfo!!.city ?: requireContext().resources.getString(R.string.str_no_data)
                val nameSurnameString = "${userInfo!!.firstName} ${userInfo!!.lastName}"
                binding.userIdentituNameSurname.text = nameSurnameString

                if (userInfo!!.userProfilePhotoUrl!!.isNotEmpty()) {
                    Glide.with(this).load(userInfo!!.userProfilePhotoUrl).diskCacheStrategy(
                        DiskCacheStrategy.RESOURCE
                    ).into(binding.userIdentityPhoto)
                } else {
                    binding.userIdentityPhoto.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_account_circle_black
                        )
                    )
                }
            }
        })
    }

    private fun setupUserMoreInfoCardFunctionality() {

        //Showing and hiding user more info card
        binding.userProfileMoreInfoButton.setOnClickListener {
            if (userMoreInfoCardOpenStatus) {
                binding.userProfileMoreInfoLayout.visibility = View.GONE
                binding.userProfileMoreInfoButton.setImageResource(R.drawable.ic_arrow_down)
                userMoreInfoCardOpenStatus = false
            } else {
                binding.userProfileMoreInfoLayout.visibility = View.VISIBLE
                binding.userProfileMoreInfoButton.setImageResource(R.drawable.ic_arrow_up)
                userMoreInfoCardOpenStatus = true
            }
        }

        //Holding edit user info button pressed
        binding.userProfileEditInfoButton.setOnClickListener {
            //Check if user have network connection
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                //Setting up edit user bottom sheet dialog fields and functionality
                setupEditUserDataDialogFields()
                setupEditUserDataDialogFunctionality()
                editUserDataDialog.show()
            } else {
                //If user does not active network connection show alert
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }
    }

    private fun setupServicesCardFunctionality() {
        binding.ordersHeaderLayout.setOnClickListener {
            view?.findNavController()
                ?.navigate(R.id.action_userProfileFragment_to_serviceOrderListFragment)
        }

        binding.pendingOrdersShortcut.setOnClickListener {
            val bundle = bundleOf(
                PARAM_SHOW_ORDER_TYPE to ORDER_PENDING
            )
            view?.findNavController()
                ?.navigate(R.id.action_userProfileFragment_to_serviceOrderListFragment, bundle)
        }

        binding.inProgressOrdersShortcut.setOnClickListener {
            val bundle = bundleOf(
                PARAM_SHOW_ORDER_TYPE to ORDER_IN_PROGRESS
            )
            view?.findNavController()
                ?.navigate(R.id.action_userProfileFragment_to_serviceOrderListFragment, bundle)
        }

        binding.doneOrdersShortcut.setOnClickListener {
            val bundle = bundleOf(
                PARAM_SHOW_ORDER_TYPE to ORDER_DONE
            )
            view?.findNavController()
                ?.navigate(R.id.action_userProfileFragment_to_serviceOrderListFragment, bundle)
        }
    }

    private fun setupEditUserDataBottomSheet() {
        editUserDataPhotoCardLayout = editUserDataBottomSheetView.findViewById(R.id.user_photo_card)
        editUserDataUserPhoto = editUserDataBottomSheetView.findViewById(R.id.add_user_photo)
        editUserDataUserFirstName =
            editUserDataBottomSheetView.findViewById(R.id.add_user_first_name)
        editUserDataUserLastName = editUserDataBottomSheetView.findViewById(R.id.add_user_last_name)
        editUserDataUserAddress = editUserDataBottomSheetView.findViewById(R.id.add_user_address)
        editUserDataUserPostalCode =
            editUserDataBottomSheetView.findViewById(R.id.add_user_postal_code)
        editUserDataUserCity = editUserDataBottomSheetView.findViewById(R.id.add_user_city)
        editUserDataUserPhoneNumber =
            editUserDataBottomSheetView.findViewById(R.id.add_user_phone_number)
        editUserDataSaveChangesButton =
            editUserDataBottomSheetView.findViewById(R.id.add_user_save_changes)
        editUserDataCloseDialogButton = editUserDataBottomSheetView.findViewById(R.id.close_dialog)

        editUserDataDialog = BottomSheetDialog(this.requireContext())
        editUserDataDialog.setContentView(editUserDataBottomSheetView)
        editUserDataDialog.setCancelable(false)
        editUserDataDialog.behavior.apply {
            isFitToContents = true
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = false
        }
    }

    private fun setupEditUserDataDialogFields() {
        if (userInfo != null) {
            Glide.with(this).load(userInfo!!.userProfilePhotoUrl).diskCacheStrategy(
                DiskCacheStrategy.RESOURCE
            ).into(editUserDataUserPhoto)

            editUserDataUserFirstName.editText?.setText(userInfo!!.firstName)
            editUserDataUserLastName.editText?.setText(userInfo!!.lastName)
            editUserDataUserAddress.editText?.setText(userInfo!!.address)
            editUserDataUserPostalCode.editText?.setText(userInfo!!.postalCode)
            editUserDataUserCity.editText?.setText(userInfo!!.city)
            editUserDataUserPhoneNumber.editText?.setText(userInfo!!.phoneNumber)
        }

    }

    private fun setupEditUserDataDialogFunctionality() {
        if (userInfo != null) {

            setupSavingDataDialog()

            editUserDataSaveChangesButton.isEnabled = false

            editUserDataPhotoCardLayout.setOnClickListener {
                addPhotoFromGallery()
            }

            editUserDataCloseDialogButton.setOnClickListener {
                editUserDataDialog.cancel()
            }

            editUserDataUserFirstName.editText?.addTextChangedListener {
                editUserDataSaveChangesButton.isEnabled =
                    it.toString() != userInfo!!.firstName!! && it?.isNotEmpty() == true
            }
            editUserDataUserLastName.editText?.addTextChangedListener {
                editUserDataSaveChangesButton.isEnabled =
                    it.toString() != userInfo!!.lastName!! && it?.isNotEmpty() == true
            }
            editUserDataUserAddress.editText?.addTextChangedListener {
                editUserDataSaveChangesButton.isEnabled =
                    it.toString() != userInfo!!.address!! && it?.isNotEmpty() == true
            }
            editUserDataUserPostalCode.editText?.addTextChangedListener {
                editUserDataSaveChangesButton.isEnabled =
                    it.toString() != userInfo!!.postalCode!! && it?.isNotEmpty() == true
            }
            editUserDataUserCity.editText?.addTextChangedListener {
                editUserDataSaveChangesButton.isEnabled =
                    it.toString() != userInfo!!.city!! && it?.isNotEmpty() == true
            }
            editUserDataUserPhoneNumber.editText?.addTextChangedListener {
                editUserDataSaveChangesButton.isEnabled =
                    it.toString() != userInfo!!.phoneNumber!! && it?.isNotEmpty() == true
            }

            editUserDataSaveChangesButton.setOnClickListener {
                val update = mutableMapOf<String, Any>()

                if (editUserDataUserFirstName.editText?.text.toString() != userInfo!!.firstName!! && editUserDataUserFirstName.editText?.text?.isNotEmpty() == true) {
                    update["firstName"] = editUserDataUserFirstName.editText?.text.toString()
                }
                if (editUserDataUserLastName.editText?.text.toString() != userInfo!!.lastName!! && editUserDataUserLastName.editText?.text?.isNotEmpty() == true) {
                    update["lastName"] = editUserDataUserLastName.editText?.text.toString()
                }
                if (editUserDataUserAddress.editText?.text.toString() != userInfo!!.address!! && editUserDataUserAddress.editText?.text?.isNotEmpty() == true) {
                    update["address"] = editUserDataUserAddress.editText?.text.toString()
                }
                if (editUserDataUserPostalCode.editText?.text.toString() != userInfo!!.postalCode!! && editUserDataUserPostalCode.editText?.text?.isNotEmpty() == true) {
                    update["postalCode"] = editUserDataUserPostalCode.editText?.text.toString()
                }
                if (editUserDataUserCity.editText?.text.toString() != userInfo!!.city!! && editUserDataUserCity.editText?.text?.isNotEmpty() == true) {
                    update["city"] = editUserDataUserCity.editText?.text.toString()
                }
                if (editUserDataUserPhoneNumber.editText?.text.toString() != userInfo!!.phoneNumber!! && editUserDataUserPhoneNumber.editText?.text?.isNotEmpty() == true) {
                    update["phoneNumber"] = editUserDataUserPhoneNumber.editText?.text.toString()
                }



                if (update.isNotEmpty() || selectedBitmap != null) {
                    uploadingDataDialog?.show()

                    firebaseRepository.updateProfileInfo(update, selectedBitmap, userInfo!!)


                    firebaseRepository.getTaskStatusListenerData()
                        .observe(viewLifecycleOwner, { status ->
                            if (status != null) {
                                if (status) {
                                    if (uploadingDataDialog != null && uploadingDataDialog!!.isShowing) {
                                        uploadingDataDialog!!.cancel()
                                        editUserDataDialog.cancel()
                                    }
                                }
                            }
                        })
                }
            }

        }
    }

    private fun setupSavingDataDialog() {
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

}

