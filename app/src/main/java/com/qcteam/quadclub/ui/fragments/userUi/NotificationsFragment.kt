package com.qcteam.quadclub.ui.fragments.userUi

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
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentNotificationsBinding
import com.xwray.groupie.GroupieAdapter

class NotificationsFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentNotificationsBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //variable holding notification recycler view adapter
    private val notificationsAdapter = GroupieAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNotificationsBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListener()

        //setting up notifications recycler view
        setupNotificationsRecyclerView()

        //holding back button press
        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()
                ?.navigate(R.id.action_userNotificationsFragment_to_userHomeFragment)
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_galleryFragment_to_userHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun setupNotificationsRecyclerView() {
        binding.notificationsRecyclerView.adapter = notificationsAdapter
        notificationsAdapter.notifyDataSetChanged()

        firebaseRepository.getNotificationsListData()
            .observe(viewLifecycleOwner, { listOfNotifications ->
                if (listOfNotifications != null && listOfNotifications.isNotEmpty()) {
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

            firebaseRepository.displayNotification(row.notification.notificationKey!!)
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}