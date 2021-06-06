package com.qcteam.quadclub.ui.fragments.companyUi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.ServiceOrder
import com.qcteam.quadclub.data.adapters.OrdersAdapterForCompany
import com.qcteam.quadclub.data.helpers.*
import com.qcteam.quadclub.data.repository.FirebaseRepositoryForCompany
import com.qcteam.quadclub.databinding.FragmentCompanyOrdersBinding

class CompanyOrdersFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding : FragmentCompanyOrdersBinding

    //FirebaseRepositoryForCompany view model variable holder
    private val firebaseRepositoryForCompany : FirebaseRepositoryForCompany by activityViewModels()

    //variable holding parameter passed by view binding
    private var paramOrderType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            paramOrderType = it.getString(PARAM_ORDER_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCompanyOrdersBinding.inflate(layoutInflater, container, false)

        //setup error listener
        setupErrorListener()

        //handling back button press
        binding.backViewButton.setOnClickListener {
            firebaseRepositoryForCompany.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_companyOrdersFragment_to_companyHomeFragment)
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepositoryForCompany.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_companyOrdersFragment_to_companyHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        //setup ui buttons functionality
        setupUiFunctionality()

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepositoryForCompany.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun setupUiFunctionality() {
        val linearLayoutManager = LinearLayoutManager(this.context)
        binding.companyOrdersRecyclerView.layoutManager = linearLayoutManager

        firebaseRepositoryForCompany.getCompanyOrdersListData().observe(viewLifecycleOwner, { orderList ->
            if(orderList != null){
                var selectedList = listOf<ServiceOrder>()
                if(paramOrderType == ORDER_PENDING){
                    selectedList = orderList.filter {
                        (!it.serviceAccept || !it.customerAccept)
                    }
                }
                if(paramOrderType == ORDER_IN_PROGRESS) {
                    selectedList = orderList.filter {
                        it.orderInProgress
                    }
                }
                if(paramOrderType == ORDER_DONE) {
                    selectedList = orderList.filter {
                        it.orderDone
                    }
                }
                val adapter = OrdersAdapterForCompany(selectedList)
                adapter.notifyDataSetChanged()
                binding.companyOrdersRecyclerView.adapter = adapter
            }
        })
    }

    override fun onStop() {
        super.onStop()
        firebaseRepositoryForCompany.resetTaskListeners()
    }

}