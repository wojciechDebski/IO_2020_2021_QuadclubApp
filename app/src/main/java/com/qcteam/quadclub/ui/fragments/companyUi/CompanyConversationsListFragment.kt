package com.qcteam.quadclub.ui.fragments.companyUi

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
import com.qcteam.quadclub.data.adapters.ConversationListRow
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_SENDER_ID
import com.qcteam.quadclub.data.helpers.PARAM_CONVERSATION_DATABASE_ID
import com.qcteam.quadclub.data.repository.FirebaseChatRepositoryForCompany
import com.qcteam.quadclub.databinding.FragmentCompanyConversationsListBinding
import com.xwray.groupie.GroupieAdapter


class CompanyConversationsListFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentCompanyConversationsListBinding

    //FirebaseRepositoryForCompany view model variable holder
    private val firebaseChatRepositoryForCompany: FirebaseChatRepositoryForCompany by activityViewModels()

    private val conversationsAdapter = GroupieAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCompanyConversationsListBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListener()

        //setting up company conversations list recycler view
        setupConversationListRecyclerView()

        //Holding back button pressed
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseChatRepositoryForCompany.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_companyConversationsListFragment_to_companyHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseChatRepositoryForCompany.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }


    private fun setupConversationListRecyclerView() {
        binding.companyConversationsRecyclerView.adapter = conversationsAdapter

        conversationsAdapter.setOnItemClickListener { item, _ ->
            val row = item as ConversationListRow
            val bundle = bundleOf(
                PARAM_CONVERSATION_DATABASE_ID to row.conversation.databaseId,
                PARAM_SENDER_ID to row.conversation.senderUid
            )

            view?.findNavController()?.navigate(
                R.id.action_companyConversationsListFragment_to_companyConversationDetailFragment,
                bundle
            )
        }

        firebaseChatRepositoryForCompany.getCompanyConversationsListData()
            .observe(viewLifecycleOwner, { conversations ->
                if (conversations != null) {
                    if (conversations.isNotEmpty()) {
                        binding.companyConversationsEmptyListText.visibility = View.GONE
                        conversationsAdapter.clear()
                        for (conversation in conversations) {
                            conversationsAdapter.add(ConversationListRow(conversation))
                        }
                        binding.companyConversationsRecyclerView.adapter = conversationsAdapter
                    } else {
                        binding.companyConversationsEmptyListText.visibility = View.VISIBLE
                    }
                } else {
                    binding.companyConversationsEmptyListText.visibility = View.VISIBLE
                }
            })
    }

    override fun onStop() {
        super.onStop()
        firebaseChatRepositoryForCompany.resetTaskListeners()
    }

}