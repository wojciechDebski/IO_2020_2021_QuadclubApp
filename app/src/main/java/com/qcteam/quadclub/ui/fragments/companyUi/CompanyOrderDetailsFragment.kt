package com.qcteam.quadclub.ui.fragments.companyUi

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.qcteam.quadclub.data.ServiceOrder
import com.qcteam.quadclub.data.adapters.StatusListAdapter
import com.qcteam.quadclub.data.adapters.StatusSpinnerAdapter
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_ORDER_NUMBER
import com.qcteam.quadclub.data.repository.FirebaseRepositoryForCompany
import com.qcteam.quadclub.databinding.FragmentCompanyOrderDetailsBinding

class CompanyOrderDetailsFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentCompanyOrderDetailsBinding

    //FirebaseRepositoryForCompany view model variable holder
    private val firebaseRepositoryForCompany: FirebaseRepositoryForCompany by activityViewModels()

    //variable holding parameter passed by view binding
    private var paramOrderNumber: Int? = null

    //variable holding selected order data class object
    private var selectedOrder: ServiceOrder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            paramOrderNumber = it.getString(PARAM_ORDER_NUMBER)?.toInt()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCompanyOrderDetailsBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListener()

        //getting selected order information and setting proper text fields
        getSelectedOrderInformationAndSetFields()

        //handling back button press
        binding.backViewButton.setOnClickListener {
            firebaseRepositoryForCompany.resetTaskListeners()
            view?.findNavController()
                ?.navigateUp()
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepositoryForCompany.resetTaskListeners()
                view?.findNavController()
                    ?.navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepositoryForCompany.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun getSelectedOrderInformationAndSetFields() {
        val linearLayoutManager = LinearLayoutManager(this.context)
        binding.orderDetailsStatusHistoryRecyclerView.layoutManager = linearLayoutManager

        val statusList = listOf(
            "Serwis oczekuje na przyjazd pojazdu",
            "Pojazd przyjęto w serwisie",
            "Rozpoczęto pracę serwisową",
            "Zakończono pracę serwisową",
            "Pojazd gotowy do odbioru",
            "Pojazd odebrany przez klienta",
            "Serwis zakończony",
            "Napotkano problemy w trakcie serwisu, skontaktuj się z nami."
        )

        val spinnerAdapter =
            StatusSpinnerAdapter(requireContext(), statusList)
        binding.orderDetailsChangeStatusSpinner.adapter = spinnerAdapter

        binding.spinnerStatusArrowBtn.setOnClickListener {
            binding.orderDetailsChangeStatusSpinner.performClick()
        }

        firebaseRepositoryForCompany.getCompanyOrdersListData()
            .observe(viewLifecycleOwner, { orderList ->
                if (orderList != null) {
                    selectedOrder = orderList.find {
                        it.orderNumber == paramOrderNumber
                    }

                    if (selectedOrder != null) {
                        if (selectedOrder?.orderDone == true) {
                            binding.orderDetailsUpdateStatusButton.isEnabled = false
                            binding.orderDetailsChangeStatusSpinner.isEnabled = false
                            binding.spinnerStatusArrowBtn.isEnabled = false
                            binding.orderDetailsNoteSaveButton.isEnabled = false
                        }

                        val adapter = StatusListAdapter(selectedOrder!!.status!!.reversed())
                        adapter.notifyDataSetChanged()
                        binding.orderDetailsStatusHistoryRecyclerView.adapter = adapter

                        if (selectedOrder!!.serviceInformation.isNotEmpty()) {
                            binding.orderDetailsNoteSavedText.text =
                                selectedOrder!!.serviceInformation
                        } else {
                            binding.orderDetailsNoteSavedText.text = "Brak notatek"
                        }

                        if (selectedOrder!!.additionalInfo.isNotEmpty()) {
                            binding.orderDetailsAdditionalInfoText.text =
                                selectedOrder!!.additionalInfo
                        } else {
                            binding.orderDetailsAdditionalInfoText.text = "brak"
                        }

                        binding.orderDetailsVehicleVinText.text =
                            selectedOrder!!.vehicle?.vehicleVinNumber
                        binding.orderDetailsOrderNoText.text =
                            selectedOrder!!.orderNumber.toString()
                        binding.orderDetailsDateTimeText.text = selectedOrder!!.dateTime
                        binding.orderDetailsVehicleModelText.text =
                            selectedOrder!!.vehicle?.vehicleModel ?: "brak danych"
                        binding.orderDetailsVehicleOwnerText.text =
                            selectedOrder!!.customerNameSurname
                        binding.orderDetailsVehicleOwnerPhoneText.text =
                            selectedOrder!!.customerPhone
                        binding.orderDetailsReportReasonText.text = selectedOrder!!.reportReason
                        binding.orderDetailsStatusText.text =
                            selectedOrder!!.status?.last()?.orderStatus ?: "brak danych"

                        binding.orderDetailsUpdateStatusButton.setOnClickListener {
                            val connectivityManager =
                                requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                            val currentNetwork = connectivityManager.activeNetwork

                            if (currentNetwork != null) {
                                firebaseRepositoryForCompany.updateOrderStatus(
                                    selectedOrder!!,
                                    binding.orderDetailsChangeStatusSpinner.selectedItem.toString()
                                )
                            } else {
                                AlertBuilder.buildNetworkAlert(requireContext())
                            }
                        }

                        binding.orderDetailsNoteSaveButton.setOnClickListener {
                            if (binding.orderDetailsNoteText.text.isNotEmpty()) {
                                val connectivityManager =
                                    requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                                val currentNetwork = connectivityManager.activeNetwork

                                if (currentNetwork != null) {
                                    firebaseRepositoryForCompany.saveNoteToOrder(
                                        selectedOrder!!,
                                        binding.orderDetailsNoteText.text.toString()
                                    )
                                } else {
                                    AlertBuilder.buildNetworkAlert(requireContext())
                                }
                            }
                            binding.orderDetailsNoteText.setText("")
                        }
                    }
                }
            })
    }


    override fun onStop() {
        super.onStop()
        firebaseRepositoryForCompany.resetTaskListeners()
    }

}