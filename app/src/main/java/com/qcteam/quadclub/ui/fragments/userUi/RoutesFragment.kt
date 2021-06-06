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
import com.qcteam.quadclub.data.adapters.RouteAdapter
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentRoutesBinding


class RoutesFragment : Fragment() {

    //binding view variable holder
    lateinit var binding: FragmentRoutesBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRoutesBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListeners()

        //setting up routes recycler view
        setupRoutesListRecyclerView()

        //Handling back press buttons actions
        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_routesFragment_to_userHomeFragment)
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()?.navigate(R.id.action_routesFragment_to_userHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupErrorListeners() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun setupRoutesListRecyclerView() {
        binding.routesListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        firebaseRepository.getUserRouteListData().observe(viewLifecycleOwner, { routesList ->
            if(routesList != null){
                binding.routesListRecyclerViewEmpty.visibility = View.GONE
                val adapter = RouteAdapter(routesList)
                adapter.notifyDataSetChanged()
                binding.routesListRecyclerView.adapter = adapter
            } else {
                binding.routesListRecyclerViewEmpty.visibility = View.VISIBLE
            }
        })
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}