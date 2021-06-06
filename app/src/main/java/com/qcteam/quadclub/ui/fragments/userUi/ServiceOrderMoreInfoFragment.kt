package com.qcteam.quadclub.ui.fragments.userUi

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.ServiceOrder
import com.qcteam.quadclub.data.adapters.StatusListAdapter
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_ORDER_NUMBER
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentServiceOrderMoreInfoBinding

class ServiceOrderMoreInfoFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentServiceOrderMoreInfoBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository : FirebaseRepository by activityViewModels()

    //Variable holding parameter passed by navigation bundle
    private var orderNumberParameter: Int? = null

    //Variable holding information about actually showing order
    private var showingOrder: ServiceOrder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            orderNumberParameter = it.getString(PARAM_ORDER_NUMBER)?.toInt()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentServiceOrderMoreInfoBinding.inflate(layoutInflater, container, false)

        setupErrorListener()

        //Setting up chosen order info in proper fields
        getShowingOrderInfoAndSetupTextFields()

        //Handling back press buttons actions
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()?.navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigateUp()
        }
        //--------------------------------------------

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null) {
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun getShowingOrderInfoAndSetupTextFields() {

        val linearLayoutManager = LinearLayoutManager(this.context)
        binding.serviceStatusHistoryRecyclerView.layoutManager = linearLayoutManager

        firebaseRepository.getUserServiceOrdersListData().observe(viewLifecycleOwner, { list ->
            showingOrder = list.find {
                it.orderNumber == orderNumberParameter
            }

            if(showingOrder != null){
                binding.serviceOrderNoText.text = showingOrder!!.orderNumber.toString()
                binding.serviceDateTimeText.text = showingOrder!!.dateTime
                binding.serviceVehicleNameText.text = showingOrder!!.vehicle?.vehicleName
                binding.serviceVehicleModelText.text = showingOrder!!.vehicle?.vehicleModel
                binding.serviceCompanyNameText.text = showingOrder!!.companyName
                binding.serviceCompanyAddressText.text = showingOrder!!.companyAddress
                binding.serviceCompanyPostalCodeText.text = showingOrder!!.companyPostalCode
                binding.serviceCompanyCityText.text = showingOrder!!.companyCity
                binding.serviceCompanyPhoneText.text = showingOrder!!.companyPhone
                binding.serviceReportReasonText.text = showingOrder!!.reportReason
                binding.serviceStatusText.text = showingOrder!!.status?.last()?.orderStatus

                if(showingOrder!!.additionalInfo.isNotEmpty()){
                    binding.serviceUserAdditionalInfoText.text = showingOrder!!.additionalInfo
                } else {
                    binding.serviceUserAdditionalInfoText.text = requireContext().resources.getString(
                        R.string.str_no_data)
                }

                if(showingOrder!!.serviceInformation.isNotEmpty()) {
                    binding.serviceAdditionalInfoFromService.text = showingOrder!!.serviceInformation
                } else {
                    binding.serviceAdditionalInfoFromService.text = requireContext().resources.getString(
                        R.string.str_no_data)
                }

                val adapter = StatusListAdapter(showingOrder!!.status!!.reversed())
                adapter.notifyDataSetChanged()
                binding.serviceStatusHistoryRecyclerView.adapter = adapter
            }
        })
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}