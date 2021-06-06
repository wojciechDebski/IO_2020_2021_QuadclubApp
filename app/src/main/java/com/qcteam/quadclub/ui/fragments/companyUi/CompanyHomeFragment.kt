package com.qcteam.quadclub.ui.fragments.companyUi

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.helpers.*
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.data.repository.FirebaseRepositoryForCompany
import com.qcteam.quadclub.databinding.FragmentCompanyHomeBinding


class CompanyHomeFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentCompanyHomeBinding

    //FirebaseRepositoryForCompany view model variable holder
    private val firebaseRepositoryForCompany: FirebaseRepositoryForCompany by activityViewModels()

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCompanyHomeBinding.inflate(layoutInflater, container, false)

        checkIfCompanyIsVerified()

        //setting up error listener
        setupErrorListener()

        //handle back button press
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepositoryForCompany.resetTaskListeners()
                activity?.moveTaskToBack(true)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        //setting up logout listener
        setupLogoutStatusListener()

        //setting up ui buttons functionality
        setupUiFunctionality()

        //getting counters of specific type of company orders
        getCompanyOrdersListCounters()

        //getting logged company information and setting it in proper text fields
        getLoggedCompanyInfoAndSetTextFields()

        return binding.root
    }

    private fun checkIfCompanyIsVerified() {
        firebaseRepositoryForCompany.getLoggedCompanyInfoData().observe(viewLifecycleOwner, { companyInfo ->
            if(companyInfo!= null) {
                if(!companyInfo.isVerified){
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(requireContext().resources.getString(R.string.caution_alert_title))
                    builder.setMessage(requireContext().resources.getString(R.string.no_verified_company))
                    builder.setPositiveButton(android.R.string.ok) { _, _ ->
                        firebaseRepository.signOutUser()
                    }
                    builder.show()
                }
            }
        })
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })

        firebaseRepositoryForCompany.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun setupLogoutStatusListener() {
        firebaseRepository.getAuthorizedUserData().observe(viewLifecycleOwner, { user ->
            if (user == null) {
                firebaseRepositoryForCompany.resetTaskListeners()
                firebaseRepository.resetTaskListeners()
                view?.findNavController()?.navigate(R.id.action_companyHomeFragment_to_loginFragment)
            }
        })
    }

    private fun setupUiFunctionality() {
        binding.logoutBtn.setOnClickListener {
            firebaseRepository.signOutUser()
        }

        binding.ordersPendingButton.setOnClickListener {
            val bundle = bundleOf(
                PARAM_ORDER_TYPE to ORDER_PENDING
            )
            view?.findNavController()
                ?.navigate(R.id.action_companyHomeFragment_to_companyOrdersFragment, bundle)
        }

        binding.ordersPendingTitle.setOnClickListener {
            val bundle = bundleOf(
                PARAM_ORDER_TYPE to ORDER_PENDING
            )
            view?.findNavController()
                ?.navigate(R.id.action_companyHomeFragment_to_companyOrdersFragment, bundle)
        }

        binding.ordersInProgressButton.setOnClickListener {
            val bundle = bundleOf(
                PARAM_ORDER_TYPE to ORDER_IN_PROGRESS
            )
            view?.findNavController()
                ?.navigate(R.id.action_companyHomeFragment_to_companyOrdersFragment, bundle)
        }

        binding.ordersInProgressTitle.setOnClickListener {
            val bundle = bundleOf(
                PARAM_ORDER_TYPE to ORDER_IN_PROGRESS
            )
            view?.findNavController()
                ?.navigate(R.id.action_companyHomeFragment_to_companyOrdersFragment, bundle)
        }

        binding.ordersDoneButton.setOnClickListener {
            val bundle = bundleOf(
                PARAM_ORDER_TYPE to ORDER_DONE
            )
            view?.findNavController()
                ?.navigate(R.id.action_companyHomeFragment_to_companyOrdersFragment, bundle)
        }

        binding.ordersDoneTitle.setOnClickListener {
            val bundle = bundleOf(
                PARAM_ORDER_TYPE to ORDER_DONE
            )
            view?.findNavController()
                ?.navigate(R.id.action_companyHomeFragment_to_companyOrdersFragment, bundle)
        }
    }

    private fun getCompanyOrdersListCounters() {
        firebaseRepositoryForCompany.getCompanyOrdersListData()
            .observe(viewLifecycleOwner, { orderList ->
                if (orderList != null) {
                    binding.ordersPendingNumber.text = orderList.filter {
                        (!it.serviceAccept || !it.customerAccept)
                    }.size.toString()

                    binding.ordersInProgressNumber.text = orderList.filter {
                        it.orderInProgress
                    }.size.toString()

                    binding.ordersDoneNumber.text = orderList.filter {
                        it.orderDone
                    }.size.toString()
                }
            })
    }

    private fun getLoggedCompanyInfoAndSetTextFields() {
        firebaseRepositoryForCompany.getLoggedCompanyInfoData()
            .observe(viewLifecycleOwner, { companyInfo ->
                if (companyInfo != null) {
                    binding.companyInfoName.text = companyInfo.companyName
                    binding.companyInfoNipText.text =
                        companyInfo.companyTaxIdentificationNumber
                    binding.companyInfoPhoneText.text = companyInfo.companyPhoneNumber
                    binding.companyInfoAddressText.text = companyInfo.companyAddress
                    binding.companyInfoPostalCodeText.text =
                        companyInfo.companyPostalCode
                    binding.companyInfoCityText.text = companyInfo.companyCity
                }

            })
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
        firebaseRepositoryForCompany.resetTaskListeners()
    }

}