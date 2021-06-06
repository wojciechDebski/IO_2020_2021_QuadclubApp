package com.qcteam.quadclub.ui.fragments.userUi

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.CompanyInfo
import com.qcteam.quadclub.data.ServiceOrder
import com.qcteam.quadclub.data.ServiceOrderStatus
import com.qcteam.quadclub.data.adapters.QuadSpinnerAdapter
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_TAX_ID
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentServiceBinding
import java.text.SimpleDateFormat
import java.util.*


class ServiceFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentServiceBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //Variable holding reference to dialog that appears during uploading data to Firebase
    private var uploadingDataDialog: AlertDialog? = null

    //Variable holding parameter passed by navigation bundle
    private var taxIdParameter: String? = null

    //Variable holding information about selected company
    private var selectedCompany: CompanyInfo? = null


    private val calendar: Calendar = Calendar.getInstance()
    var selectedDateTime: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            taxIdParameter = it.getString(PARAM_TAX_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentServiceBinding.inflate(layoutInflater, container, false)

        //Setting up listeners for errors and uploading service order status
        setupErrorListener()
        setupUploadingServiceOrderStatusListener()

        //Setting up chosen company info in proper fields
        getSelectedCompanyInfoAndSetupTextFields()

        //Setting up spinner with user vehicles
        getUserVehicleListAndSetupSpinner()

        //Setting up functionality of service order form
        setupFormFunctionality()

        //Handling back press buttons actions
        binding.serviceOrderCloseButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_serviceFragment_to_userHomeFragment)
        }
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()?.navigate(R.id.action_serviceFragment_to_userHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

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

    private fun setupUploadingServiceOrderStatusListener() {
        firebaseRepository.getTaskStatusListenerData()
            .observe(viewLifecycleOwner, { status ->
                if (status != null && uploadingDataDialog != null) {
                    if (status) {
                        if (uploadingDataDialog!!.isShowing) {
                            uploadingDataDialog!!.cancel()
                        }
                        firebaseRepository.resetTaskListeners()
                        view?.findNavController()
                            ?.navigate(R.id.action_serviceFragment_to_serviceOrderListFragment)
                    }
                }
            })
    }

    private fun getSelectedCompanyInfoAndSetupTextFields() {
        if (taxIdParameter != null) {
            firebaseRepository.getCompanyListData().observe(viewLifecycleOwner, { companyList ->
                if (companyList != null) {
                    selectedCompany = companyList.find { companyInfo ->
                        companyInfo.companyTaxIdentificationNumber == taxIdParameter
                    }

                    if (selectedCompany != null) {
                        binding.appointmentServiceName.setText(selectedCompany!!.companyName)
                        val oneLineAddress: String =
                            selectedCompany!!.companyAddress + ", " + selectedCompany!!.companyPostalCode + " " + selectedCompany!!.companyCity + "\ntel:" + selectedCompany!!.companyPhoneNumber + ", NIP: " + selectedCompany!!.companyTaxIdentificationNumber
                        binding.appointmentServiceFullAddress.setText(oneLineAddress)
                    }
                }
            })
        }
    }

    private fun getUserVehicleListAndSetupSpinner() {
        firebaseRepository.getUserVehicleListData().observe(viewLifecycleOwner, { vehicleList ->
            if (vehicleList != null) {
                val spinnerAdapter =
                    QuadSpinnerAdapter(requireContext(), vehicleList)
                spinnerAdapter.notifyDataSetChanged()
                binding.appointmentVehicleSpinner.adapter = spinnerAdapter

                binding.appointmentSendTheApplication.isEnabled = vehicleList.isNotEmpty()

                if(vehicleList.isEmpty()){
                    AlertBuilder.buildEmptyVehicleListAlert(requireContext())
                }
            }
        })
    }

    private fun setupFormFunctionality() {
        setDatePicker()
        setTimePicker()

        binding.appointmentSendTheApplication.setOnClickListener {
            val connectivityManager =
                view?.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                setupUploadingOrderDialog()
                checkAndSendApplication()
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }
    }

    private fun setupUploadingOrderDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_uplading_data, null)
        val alertBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        alertBuilder.setCancelable(false)
        uploadingDataDialog = alertBuilder.create()
    }

    private fun checkAndSendApplication() {

        var validator = 0
        validator += checkEditText(binding.appointmentSelectDate)
        validator += checkEditText(binding.appointmentSelectTime)
        validator += checkEditText(binding.appointmentReportReason)
        validator += if (binding.appointmentTermsAndConditions.isChecked) {
            1
        } else {
            Toast.makeText(
                requireContext(),
                requireContext().resources.getString(R.string.str_acceptance_of_the_regulations_required),
                Toast.LENGTH_SHORT
            )
                .show()
            0
        }

        if (validator == 4) {
            val quad =
                firebaseRepository.getUserVehicleListData().value?.get(binding.appointmentVehicleSpinner.selectedItemPosition)!!
            var order: ServiceOrder? = null
            firebaseRepository.getAuthorizedUserData().value?.let { customer ->
                firebaseRepository.getLoggedUserInfoData().value?.let { userInfo ->
                    if (selectedCompany != null) {
                        order = ServiceOrder(
                            1,
                            selectedCompany!!.uid!!,
                            selectedCompany!!.companyName!!,
                            selectedCompany!!.companyAddress!!,
                            selectedCompany!!.companyPostalCode!!,
                            selectedCompany!!.companyCity!!,
                            selectedCompany!!.companyTaxIdentificationNumber!!,
                            selectedCompany!!.companyPhoneNumber!!,
                            customer.uid,
                            "${userInfo.firstName} ${userInfo.lastName}",
                            userInfo.phoneNumber,
                            selectedCompany!!.companyTaxIdentificationNumber!!,
                            selectedDateTime,
                            binding.appointmentReportReason.text.trim().toString(),
                            binding.appointmentAdditionalInfo.text.trim().toString(),
                            quad,
                            listOf(
                                ServiceOrderStatus(
                                    requireContext().resources.getString(R.string.str_the_order_is_awaiting_approval)
                                )
                            )
                        )
                    }
                }
            }
            if (order != null && uploadingDataDialog != null) {
                uploadingDataDialog!!.show()
                firebaseRepository.saveServiceOrder(order!!)
            }
        }
    }

    private fun checkEditText(field: EditText): Int {
        return if (field.text.isNotEmpty()) {
            field.error = null
            1
        } else {
            field.error = resources.getString(R.string.enter_proper_data_msg)
            field.addTextChangedListener {
                field.error = null
            }
            0
        }
    }

    private fun setTimePicker() {
        binding.appointmentSelectTime.setOnClickListener {
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val timeString =
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
                selectedDateTime += " " + String.format(
                    resources.getString(R.string.str_hour_plus_time_template), timeString
                )
                binding.appointmentSelectTime.setText(timeString)
            }
            TimePickerDialog(
                requireContext(),
                timeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun setDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        binding.appointmentSelectDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, _year, _month, _dayOfMonth ->
                    val dateString = "$_dayOfMonth.$_month.$_year"
                    selectedDateTime = dateString
                    binding.appointmentSelectDate.setText(dateString)
                },
                year,
                month,
                day
            )
            datePickerDialog.datePicker.minDate = calendar.time.time
            datePickerDialog.show()
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}