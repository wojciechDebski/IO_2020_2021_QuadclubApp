package com.qcteam.quadclub.ui.fragments.userUi

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
import com.qcteam.quadclub.data.adapters.OrdersAdapter
import com.qcteam.quadclub.data.helpers.*
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentServiceOrderListBinding



class ServiceOrderListFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentServiceOrderListBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //Variable holding parameter passed by navigation bundle
    private var showParameter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            showParameter = it.getString(PARAM_SHOW_ORDER_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentServiceOrderListBinding.inflate(layoutInflater, container, false)

        setupErrorListener()

        //Setting up recycler view with service orders list
        setupServiceOrderListRecyclerView()

        //setting up visible orders if user selected specific type of order in userProfileFragment
        if (showParameter != null) {
            when (showParameter.toString()) {
                ORDER_PENDING -> {
                    binding.radioGroup.check(R.id.orders_pending)
                    setupPendingOrderListRecyclerView()
                }
                ORDER_IN_PROGRESS -> {
                    binding.radioGroup.check(R.id.orders_in_progress)
                    setupInProgressOrderListRecyclerView()
                }
                ORDER_DONE -> {
                    binding.radioGroup.check(R.id.orders_done)
                    setupDoneOrderListRecyclerView()
                }
            }
        }

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

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(),error)
            }
        })
    }

    private fun setupDoneOrderListRecyclerView() {
        firebaseRepository.getUserServiceOrdersListData()
            .observe(viewLifecycleOwner, { ordersList ->
                if (ordersList != null) {
                    var adapter = OrdersAdapter(ordersList)
                    adapter.notifyDataSetChanged()
                    binding.ordersListRecyclerAll.adapter = adapter

                    val selectedOrders = ordersList.filter { order ->
                        order.orderDone
                    }

                    adapter = OrdersAdapter(selectedOrders)
                    binding.ordersListRecyclerAll.adapter = adapter
                }
            })
    }

    private fun setupInProgressOrderListRecyclerView() {
        firebaseRepository.getUserServiceOrdersListData()
            .observe(viewLifecycleOwner, { ordersList ->
                if (ordersList != null) {
                    var adapter = OrdersAdapter(ordersList)
                    adapter.notifyDataSetChanged()
                    binding.ordersListRecyclerAll.adapter = adapter

                    val selectedOrders = ordersList.filter { order ->
                        order.orderInProgress
                    }

                    adapter = OrdersAdapter(selectedOrders)
                    binding.ordersListRecyclerAll.adapter = adapter
                }
            })
    }

    private fun setupPendingOrderListRecyclerView() {
        firebaseRepository.getUserServiceOrdersListData()
            .observe(viewLifecycleOwner, { ordersList ->
                if (ordersList != null) {
                    var adapter = OrdersAdapter(ordersList)
                    adapter.notifyDataSetChanged()
                    binding.ordersListRecyclerAll.adapter = adapter
                    
                    val selectedOrders = ordersList.filter { order ->
                        (!order.customerAccept || !order.serviceAccept)
                    }
                    
                    adapter = OrdersAdapter(selectedOrders)
                    binding.ordersListRecyclerAll.adapter = adapter
                }
            })
    }

    private fun setupServiceOrderListRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(this.context)
        binding.ordersListRecyclerAll.layoutManager = linearLayoutManager

        firebaseRepository.getUserServiceOrdersListData()
            .observe(viewLifecycleOwner, { ordersList ->
                if (ordersList != null) {
                    var adapter = OrdersAdapter(ordersList)
                    adapter.notifyDataSetChanged()
                    binding.ordersListRecyclerAll.adapter = adapter

                    var selectedOrders = listOf<ServiceOrder>()
                    binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
                        when (checkedId) {
                            R.id.orders_all -> {
                                adapter = OrdersAdapter(ordersList)
                                binding.ordersListRecyclerAll.adapter = adapter
                                return@setOnCheckedChangeListener
                            }
                            R.id.orders_pending -> {
                                selectedOrders = ordersList.filter { order ->
                                    (!order.customerAccept || !order.serviceAccept)
                                }
                            }
                            R.id.orders_in_progress -> {
                                selectedOrders = ordersList.filter { order ->
                                    order.orderInProgress
                                }
                            }
                            R.id.orders_done -> {
                                selectedOrders = ordersList.filter { order ->
                                    order.orderDone
                                }
                            }
                        }
                        adapter = OrdersAdapter(selectedOrders)
                        binding.ordersListRecyclerAll.adapter = adapter
                    }
                }
            })
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}