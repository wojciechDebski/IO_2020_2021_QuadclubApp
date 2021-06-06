package com.qcteam.quadclub.ui.fragments.companyUi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.adapters.NotificationsListRow
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.repository.FirebaseRepositoryForCompany
import com.qcteam.quadclub.databinding.FragmentCompanyNotificationsBinding
import com.xwray.groupie.GroupieAdapter


class CompanyNotificationsFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentCompanyNotificationsBinding

    //FirebaseRepositoryForCompany view model variable holder
    private val firebaseRepositoryForCompany: FirebaseRepositoryForCompany by activityViewModels()

    private val notificationsAdapter = GroupieAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCompanyNotificationsBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListener()

        //getting notifications for logged company and displayed their in recycler adapter
        getNotificationsListAndSetupRecyclerAdapter()

        //handling back button press
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepositoryForCompany.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_companyNotificationsFragment_to_companyHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepositoryForCompany.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun getNotificationsListAndSetupRecyclerAdapter() {
        binding.notificationsRecyclerView.adapter = notificationsAdapter
        notificationsAdapter.notifyDataSetChanged()

        firebaseRepositoryForCompany.getNotificationsListData().observe(viewLifecycleOwner, { listOfNotifications ->
            if(listOfNotifications != null && listOfNotifications.isNotEmpty()){
                binding.notificationsEmptyListText.visibility = View.GONE
                notificationsAdapter.clear()
                for (notification in listOfNotifications) {
                    notificationsAdapter.add(NotificationsListRow(notification))
                }
                binding.notificationsRecyclerView.adapter = notificationsAdapter
            } else {
                notificationsAdapter.clear()
                binding.notificationsEmptyListText.visibility = View.VISIBLE
            }
        })

        notificationsAdapter.setOnItemClickListener { item, _ ->
            val row = item as NotificationsListRow
            firebaseRepositoryForCompany.displayNotification(row.notification.notificationKey!!)
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseRepositoryForCompany.resetTaskListeners()
    }
}