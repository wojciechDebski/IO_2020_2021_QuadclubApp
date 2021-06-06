package com.qcteam.quadclub.ui.fragments

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.CompanyInfo
import com.qcteam.quadclub.data.UserInfo
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.AppHelper
import com.qcteam.quadclub.data.repository.FirebaseChatRepository
import com.qcteam.quadclub.data.repository.FirebaseChatRepositoryForCompany
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.data.repository.FirebaseRepositoryForCompany
import com.qcteam.quadclub.databinding.FragmentRegisterBinding


class RegisterFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentRegisterBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //FirebaseRepositoryForCompany view model variable holder
    private val firebaseRepositoryForCompany: FirebaseRepositoryForCompany by activityViewModels()

    //FirebaseChatRepository view model variable holder
    private val firebaseChatRepository: FirebaseChatRepository by activityViewModels()

    //FirebaseChatRepositoryForCompany view model variable holder
    private val firebaseChatRepositoryForCompany: FirebaseChatRepositoryForCompany by activityViewModels()

    private var createCreatingAccountDialog: AlertDialog? = null

    //Variable holding bitmap of selected vehicle photo
    private var selectedBitmap: Bitmap? = null

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
                binding.registrationUserPhotoImageHolder.setImageBitmap(selectedBitmap)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)

        setupErrorListeners()

        setupCheckingIfUserIsLoggedAndStartingGettingDataFromFirebase()

        setupUiFunctionality()

        //Handling back press buttons actions
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                firebaseRepositoryForCompany.resetTaskListeners()
                firebaseChatRepository.resetTaskListeners()
                firebaseChatRepositoryForCompany.resetTaskListeners()
                view?.findNavController()?.navigate(R.id.action_registerFragment_to_loginFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        binding.registrationBackButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            firebaseRepositoryForCompany.resetTaskListeners()
            firebaseChatRepository.resetTaskListeners()
            firebaseChatRepositoryForCompany.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_registerFragment_to_loginFragment)
        }

        return binding.root
    }

    private fun setupCheckingIfUserIsLoggedAndStartingGettingDataFromFirebase() {
        firebaseRepository.getAuthorizedUserData().observe(viewLifecycleOwner, { user ->
            if (user != null) {
                firebaseRepository.checkIfCompany()
                firebaseRepository.getIsCompanyUserData().observe(viewLifecycleOwner, { isCompany ->
                    if (isCompany != null) {
                        if (isCompany) {
                            createCreatingAccountDialog?.cancel()

                            firebaseRepositoryForCompany.getCompanyOrders()
                            firebaseRepositoryForCompany.getLoggedCompanyInfo()
                            firebaseRepositoryForCompany.getNotificationsList()
                            firebaseChatRepositoryForCompany.getConversationsList()

                            view?.findNavController()
                                ?.navigate(R.id.action_registerFragment_to_companyHomeFragment)
                        } else {
                            createCreatingAccountDialog?.cancel()

                            firebaseRepository.getVehicleList()
                            firebaseRepository.getCurrentUserInfo()
                            firebaseRepository.getUserServiceOrdersList()
                            firebaseRepository.getOrderCounter()
                            firebaseRepository.getPostsList()
                            firebaseRepository.getNotificationsList()
                            firebaseChatRepository.getConversationsList()
                            firebaseRepository.getUserRoutesList()

                            view?.findNavController()
                                ?.navigate(R.id.action_registerFragment_to_userHomeFragment)
                        }
                    }
                })
            }
        })
    }

    private fun setupUiFunctionality() {
        //change visible forms according to checked checkbox
        binding.registrationAsServiceCheckbox.setOnClickListener {
            if (binding.registrationAsServiceCheckbox.isChecked) {
                binding.registrationAsServiceForm.visibility = View.VISIBLE

                binding.registrationCompanyAddressCheckbox.setOnClickListener {
                    if (binding.registrationCompanyAddressCheckbox.isChecked) {
                        binding.registrationCompanyAddressForm.visibility = View.GONE
                    } else {
                        binding.registrationCompanyAddressForm.visibility = View.VISIBLE
                    }
                }

                binding.registrationCompanyPhoneCheckbox.setOnClickListener {
                    if (binding.registrationCompanyPhoneCheckbox.isChecked) {
                        binding.registrationCompanyPhoneForm.visibility = View.GONE
                    } else {
                        binding.registrationCompanyPhoneForm.visibility = View.VISIBLE
                    }
                }
            } else {
                binding.registrationAsServiceForm.visibility = View.GONE
            }
        }

        binding.registrationUserPhotoCard.setOnClickListener {
            addPhotoFromGallery()
        }


        binding.registrationCreateAccountButton.setOnClickListener {
            setupCreatingAccountDialog()

            if (binding.registrationAsServiceCheckbox.isChecked && !binding.registrationCompanyAddressCheckbox.isChecked && !binding.registrationCompanyPhoneCheckbox.isChecked) {
                val result = checkAllFields()
                if (result == 16) {
                    binding.registrationUserPhotoImageHolder.background = null
                    val userInfo = UserInfo(
                        "", "",
                        binding.registrationFirstName.editText?.text?.trim().toString(),
                        binding.registrationLastName.editText?.text?.trim().toString(),
                        binding.registrationEmailAdress.editText?.text?.trim().toString(),
                        binding.registrationPhoneNumber.editText?.text?.trim().toString(),
                        binding.registrationAddress.editText?.text?.trim().toString(),
                        binding.registrationPostalCode.editText?.text?.trim().toString(),
                        binding.registrationCity.editText?.text?.trim().toString(),
                        binding.registrationAsServiceCheckbox.isChecked
                    )
                    val companyInfo = CompanyInfo(
                        "",
                        userInfo,
                        binding.registrationCompanyName.editText?.text?.trim().toString(),
                        "PL" + binding.registrationCompanyTaxIdentificationNumber.editText?.text?.trim()
                            .toString(),
                        binding.registrationCompanyAddress.editText?.text?.trim().toString(),
                        binding.registrationCompanyPostalCode.editText?.text?.trim().toString(),
                        binding.registrationCompanyCity.editText?.text?.trim().toString(),
                        binding.registrationCompanyPhone.editText?.text?.trim().toString()
                    )

                    createCreatingAccountDialog?.show()

                    firebaseRepository.registerUserAndSaveInfo(
                        binding.registrationPassword.editText?.text?.trim().toString(),
                        userInfo,
                        selectedBitmap,
                        companyInfo
                    )


                }
            } else if (binding.registrationAsServiceCheckbox.isChecked && binding.registrationCompanyAddressCheckbox.isChecked && !binding.registrationCompanyPhoneCheckbox.isChecked) {
                val result = checkWithoutCompanyAddress()
                if (result == 13) {
                    binding.registrationUserPhotoImageHolder.background = null
                    val userInfo = UserInfo(
                        "", "",
                        binding.registrationFirstName.editText?.text?.trim().toString(),
                        binding.registrationLastName.editText?.text?.trim().toString(),
                        binding.registrationEmailAdress.editText?.text?.trim().toString(),
                        binding.registrationPhoneNumber.editText?.text?.trim().toString(),
                        binding.registrationAddress.editText?.text?.trim().toString(),
                        binding.registrationPostalCode.editText?.text?.trim().toString(),
                        binding.registrationCity.editText?.text?.trim().toString(),
                        binding.registrationAsServiceCheckbox.isChecked
                    )
                    val companyInfo = CompanyInfo(
                        "",
                        userInfo,
                        binding.registrationCompanyName.editText?.text?.trim().toString(),
                        "PL" + binding.registrationCompanyTaxIdentificationNumber.editText?.text?.trim()
                            .toString(),
                        userInfo.address!!,
                        userInfo.postalCode!!,
                        userInfo.city!!,
                        binding.registrationCompanyPhone.editText?.text?.trim().toString()
                    )

                    createCreatingAccountDialog?.show()

                    firebaseRepository.registerUserAndSaveInfo(
                        binding.registrationPassword.editText?.text?.trim().toString(),
                        userInfo,
                        selectedBitmap,
                        companyInfo
                    )


                }
            } else if (binding.registrationAsServiceCheckbox.isChecked && binding.registrationCompanyAddressCheckbox.isChecked && binding.registrationCompanyPhoneCheckbox.isChecked) {
                val result = checkWithoutCompanyAddressAndPhone()
                if (result == 12) {
                    binding.registrationUserPhotoImageHolder.background = null
                    val userInfo = UserInfo(
                        "", "",
                        binding.registrationFirstName.editText?.text?.trim().toString(),
                        binding.registrationLastName.editText?.text?.trim().toString(),
                        binding.registrationEmailAdress.editText?.text?.trim().toString(),
                        binding.registrationPhoneNumber.editText?.text?.trim().toString(),
                        binding.registrationAddress.editText?.text?.trim().toString(),
                        binding.registrationPostalCode.editText?.text?.trim().toString(),
                        binding.registrationCity.editText?.text?.trim().toString(),
                        binding.registrationAsServiceCheckbox.isChecked
                    )
                    val companyInfo = CompanyInfo(
                        "",
                        userInfo,
                        binding.registrationCompanyName.editText?.text?.trim().toString(),
                        "PL" + binding.registrationCompanyTaxIdentificationNumber.editText?.text?.trim()
                            .toString(),
                        userInfo.address!!,
                        userInfo.postalCode!!,
                        userInfo.city!!,
                        userInfo.phoneNumber!!
                    )

                    createCreatingAccountDialog?.show()

                    firebaseRepository.registerUserAndSaveInfo(
                        binding.registrationPassword.editText?.text?.trim().toString(),
                        userInfo,
                        selectedBitmap,
                        companyInfo
                    )


                }
            } else {
                val result = checkOnlyUser()
                if (result == 10) {
                    binding.registrationUserPhotoImageHolder.background = null
                    val userInfo = UserInfo(
                        "", "",
                        binding.registrationFirstName.editText?.text?.trim().toString(),
                        binding.registrationLastName.editText?.text?.trim().toString(),
                        binding.registrationEmailAdress.editText?.text?.trim().toString(),
                        binding.registrationPhoneNumber.editText?.text?.trim().toString(),
                        binding.registrationAddress.editText?.text?.trim().toString(),
                        binding.registrationPostalCode.editText?.text?.trim().toString(),
                        binding.registrationCity.editText?.text?.trim().toString(),
                        binding.registrationAsServiceCheckbox.isChecked
                    )

                    createCreatingAccountDialog?.show()

                    firebaseRepository.registerUserAndSaveInfo(
                        binding.registrationPassword.editText?.text?.trim().toString(),
                        userInfo,
                        selectedBitmap,
                        null
                    )


                }
            }
        }
    }

    private fun setupCreatingAccountDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_creating_account, null)
        val alertBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        alertBuilder.setCancelable(false)
        createCreatingAccountDialog = alertBuilder.create()
    }

    private fun setupErrorListeners() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                if (createCreatingAccountDialog != null && createCreatingAccountDialog!!.isShowing) {
                    createCreatingAccountDialog!!.cancel()

                    AlertBuilder.buildErrorAlert(requireContext(), error)
                } else {
                    AlertBuilder.buildErrorAlert(requireContext(), error)
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
        firebaseRepositoryForCompany.resetTaskListeners()
        firebaseChatRepository.resetTaskListeners()
        firebaseChatRepositoryForCompany.resetTaskListeners()
    }


    private fun checkOnlyUser(): Int {
        var validate = 0
        validate += checkFirstName()
        validate += checkLastName()
        validate += checkEmail()
        validate += checkPhone()
        validate += checkAddress()
        validate += checkPostalCode()
        validate += checkCity()
        validate += checkPasswords()
        return validate
    }

    private fun checkWithoutCompanyAddressAndPhone(): Any {
        var validate = 0
        validate += checkFirstName()
        validate += checkLastName()
        validate += checkEmail()
        validate += checkPhone()
        validate += checkAddress()
        validate += checkPostalCode()
        validate += checkCity()
        validate += checkPasswords()
        validate += checkCompanyName()
        validate += checkNipNumber()
        return validate
    }

    private fun checkWithoutCompanyAddress(): Any {
        var validate = 0
        validate += checkFirstName()
        validate += checkLastName()
        validate += checkEmail()
        validate += checkPhone()
        validate += checkAddress()
        validate += checkPostalCode()
        validate += checkCity()
        validate += checkPasswords()
        validate += checkCompanyName()
        validate += checkNipNumber()
        validate += checkCompanyPhone()
        return validate
    }

    private fun checkAllFields(): Int {
        var validate = 0
        validate += checkFirstName()
        validate += checkLastName()
        validate += checkEmail()
        validate += checkPhone()
        validate += checkAddress()
        validate += checkPostalCode()
        validate += checkCity()
        validate += checkPasswords()
        validate += checkCompanyName()
        validate += checkNipNumber()
        validate += checkCompanyAddress()
        validate += checkCompanyPostalCode()
        validate += checkCompanyCity()
        validate += checkCompanyPhone()
        return validate
    }

    private fun checkCompanyPhone(): Int {
        val phone = binding.registrationCompanyPhone.editText?.text?.trim().toString()
        return if (phone.isEmpty()) {
            binding.registrationCompanyPhone.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationCompanyPhone.error = null
            1
        }
    }

    private fun checkCompanyCity(): Int {
        val city = binding.registrationCompanyCity.editText?.text?.trim().toString()
        return if (city.isEmpty()) {
            binding.registrationCompanyCity.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationCompanyCity.error = null
            1
        }
    }

    private fun checkCompanyPostalCode(): Int {
        val postalCode = binding.registrationCompanyPostalCode.editText?.text?.trim().toString()
        return if (postalCode.isEmpty()) {
            binding.registrationCompanyPostalCode.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationCompanyPostalCode.error = null
            1
        }
    }

    private fun checkCompanyAddress(): Int {
        val address = binding.registrationCompanyAddress.editText?.text?.trim().toString()
        return if (address.isEmpty()) {
            binding.registrationCompanyAddress.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationCompanyAddress.error = null
            1
        }
    }

    private fun checkNipNumber(): Int {
        val nipNumber =
            binding.registrationCompanyTaxIdentificationNumber.editText?.text?.trim().toString()
        val weights = arrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)
        if (nipNumber.length != 10) {
            binding.registrationCompanyTaxIdentificationNumber.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            return 0
        } else {
            var sum = 0
            for (n in 0..8) {
                sum += nipNumber[n].toString().toInt() * weights[n]
            }
            return if (sum % 11 == nipNumber[9].toString().toInt()) {
                binding.registrationCompanyTaxIdentificationNumber.error = null
                1
            } else {
                binding.registrationCompanyTaxIdentificationNumber.error =
                    requireContext().resources.getString(R.string.str_mandatory_field)
                0
            }
        }
    }

    private fun checkCompanyName(): Int {
        val companyName = binding.registrationCompanyName.editText?.text?.trim().toString()
        return if (companyName.isEmpty()) {
            binding.registrationCompanyName.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationCompanyName.error = null
            1
        }
    }

    private fun checkCity(): Int {
        val city = binding.registrationCity.editText?.text?.trim().toString()
        return if (city.isEmpty()) {
            binding.registrationCity.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationCity.error = null
            1
        }
    }

    private fun checkPhone(): Int {
        val phone = binding.registrationPhoneNumber.editText?.text?.trim().toString()
        return if (phone.isEmpty()) {
            binding.registrationPhoneNumber.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationPhoneNumber.error = null
            1
        }
    }

    private fun checkPasswords(): Int {
        val password = binding.registrationPassword.editText?.text?.trim().toString()
        val repeatedPassword =
            binding.registrationPasswordRepeated.editText?.text?.trim().toString()
        var result = 0

        result += if (password.isEmpty()) {
            binding.registrationPassword.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationPassword.error = null
            1
        }

        result += if (repeatedPassword.isEmpty()) {
            binding.registrationPasswordRepeated.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationPasswordRepeated.error = null
            1
        }

        if (password.isNotEmpty() && repeatedPassword.isNotEmpty()) {
            result += when {
                password.length < 8 -> {
                    binding.registrationPassword.error =
                        requireContext().resources.getString(R.string.str_password_must_have_eight_digits)
                    binding.registrationPasswordRepeated.error =
                        requireContext().resources.getString(R.string.str_password_must_have_eight_digits)
                    0
                }
                password != repeatedPassword -> {
                    binding.registrationPassword.error =
                        requireContext().resources.getString(R.string.str_passwords_must_be_identical)
                    binding.registrationPasswordRepeated.error =
                        requireContext().resources.getString(R.string.str_passwords_must_be_identical)
                    0
                }
                else -> {
                    binding.registrationPassword.error = null
                    binding.registrationPasswordRepeated.error = null
                    1
                }
            }
        }
        return result
    }

    private fun checkPostalCode(): Int {
        val postalCode = binding.registrationPostalCode.editText?.text?.trim().toString()
        return if (postalCode.isEmpty()) {
            binding.registrationPostalCode.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationPostalCode.error = null
            1
        }
    }

    private fun checkAddress(): Int {
        val address = binding.registrationAddress.editText?.text?.trim().toString()
        return if (address.isEmpty()) {
            binding.registrationAddress.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationAddress.error = null
            1
        }
    }

    private fun checkEmail(): Int {
        val email = binding.registrationEmailAdress.editText?.text?.trim().toString()
        return if (email.isEmpty()) {
            binding.registrationEmailAdress.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            return if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.registrationEmailAdress.error =
                    requireContext().resources.getString(R.string.str_wrong_address_email)
                0
            } else {
                binding.registrationEmailAdress.error = null
                1
            }
        }
    }

    private fun checkLastName(): Int {
        val lastName = binding.registrationLastName.editText?.text?.trim().toString()
        return if (lastName.isEmpty()) {
            binding.registrationLastName.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationLastName.error = null
            1
        }
    }

    private fun checkFirstName(): Int {
        val firstName = binding.registrationFirstName.editText?.text?.trim().toString()
        return if (firstName.isEmpty()) {
            binding.registrationFirstName.error =
                requireContext().resources.getString(R.string.str_mandatory_field)
            0
        } else {
            binding.registrationFirstName.error = null
            1
        }
    }

    private fun addPhotoFromGallery() {
        getContent.launch("image/*")
    }


}