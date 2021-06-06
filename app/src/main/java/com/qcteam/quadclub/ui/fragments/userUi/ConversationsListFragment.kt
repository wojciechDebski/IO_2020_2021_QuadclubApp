package com.qcteam.quadclub.ui.fragments.userUi

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.UserInfo
import com.qcteam.quadclub.data.adapters.ConversationListRow
import com.qcteam.quadclub.data.adapters.UsersListRow
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_CONVERSATION_DATABASE_ID
import com.qcteam.quadclub.data.helpers.PARAM_SENDER_ID
import com.qcteam.quadclub.data.repository.FirebaseChatRepository
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentConversationsListBinding
import com.xwray.groupie.GroupieAdapter


class ConversationsListFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentConversationsListBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //FirebaseChatRepository view model variable holder
    private val firebaseChatRepository: FirebaseChatRepository by activityViewModels()

    //variable holding logged user data class object
    private var loggedUser: UserInfo? = null

    //variables holding adapters fo recycler views
    private val privateConversationsAdapter = GroupieAdapter()
    private val companyConversationsAdapter = GroupieAdapter()
    private val usersListAdapter = GroupieAdapter()

    //variables responsible for start new conversation bottom sheet dialog
    private lateinit var startNewConversationBottomSheet: View
    private lateinit var startNewConversationBottomSheetDialog: BottomSheetDialog
    private lateinit var startNewConversationSearchView: SearchView
    private lateinit var startNewConversationUsersListRecyclerView: RecyclerView
    private lateinit var startNewConversationCancelBottomSheet: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentConversationsListBinding.inflate(layoutInflater, container, false)

        startNewConversationBottomSheet = layoutInflater.inflate(R.layout.start_new_conversation_bottom_sheet, null)

        //setting up error listener
        setupErrorListener()

        //set up searching users for conversation bottom sheet dialog
        setupBottomSheet()

        //getting logged user information data class object from Firebase
        getLoggedUserInfo()

        //setting up ui buttons click listeners and functions
        setupUiFunctionality()

        //handling back button press
        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            firebaseChatRepository.resetTaskListeners()
            view?.findNavController()
                ?.navigate(R.id.action_userConversationsListFragment_to_userHomeFragment)
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                firebaseChatRepository.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_userConversationsListFragment_to_userHomeFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })

        firebaseChatRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun setupBottomSheet() {
        firebaseChatRepository.getUsersForConversationList()

        startNewConversationSearchView = startNewConversationBottomSheet.findViewById(R.id.new_conversation_search_view)
        startNewConversationUsersListRecyclerView = startNewConversationBottomSheet.findViewById(R.id.new_conversation_recycler_view)
        startNewConversationCancelBottomSheet = startNewConversationBottomSheet.findViewById(R.id.new_conversation_cancel)

        startNewConversationCancelBottomSheet.setOnClickListener {
            if (startNewConversationBottomSheetDialog.isShowing) {
                startNewConversationBottomSheetDialog.cancel()
            }
        }

        startNewConversationBottomSheetDialog = BottomSheetDialog(requireContext())
        startNewConversationBottomSheetDialog.apply {
            setContentView(startNewConversationBottomSheet)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            startNewConversationUsersListRecyclerView.requestFocus()
        }
        startNewConversationBottomSheetDialog.behavior.apply {
            isFitToContents = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }


        usersListAdapter.setOnItemClickListener { item, _ ->
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                val row = item as UsersListRow

                firebaseChatRepository.getUserConversationsListData()
                    .observe(viewLifecycleOwner, { conversations ->
                        if (conversations != null) {
                            if (conversations.isNotEmpty()) {
                                val confa = conversations.find {
                                    it.senderUid == row.user.userUid
                                }

                                if (confa != null) {
                                    val bundle = bundleOf(
                                        PARAM_CONVERSATION_DATABASE_ID to confa.databaseId,
                                        PARAM_SENDER_ID to row.user.userUid
                                    )
                                    view?.findNavController()?.navigate(
                                        R.id.action_conversationsListFragment_to_conversationDetailsFragment,
                                        bundle
                                    )
                                } else {
                                    firebaseChatRepository.startNewConversation(
                                        row.user,
                                        firebaseRepository.getLoggedUserInfoData().value!!
                                    )
                                }
                            } else {
                                firebaseChatRepository.startNewConversation(
                                    row.user,
                                    firebaseRepository.getLoggedUserInfoData().value!!
                                )
                            }
                        }
                    })


                firebaseChatRepository.getNewConversationStatusData()
                    .observe(viewLifecycleOwner, { databaseId ->
                        if (databaseId != null) {
                            if (databaseId.isNotEmpty()) {
                                val bundle = bundleOf(
                                    PARAM_CONVERSATION_DATABASE_ID to databaseId,
                                    PARAM_SENDER_ID to row.user.userUid
                                )
                                view?.findNavController()?.navigate(
                                    R.id.action_conversationsListFragment_to_conversationDetailsFragment,
                                    bundle
                                )
                            }
                        }
                    })
                startNewConversationBottomSheetDialog.cancel()

            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }
    }

    private fun getLoggedUserInfo() {
        loggedUser = firebaseRepository.getLoggedUserInfoData().value
    }

    private fun setupUiFunctionality() {
        binding.conversationsWithCompanyRecyclerView.adapter = companyConversationsAdapter
        binding.conversationsRecyclerView.adapter = privateConversationsAdapter

        privateConversationsAdapter.setOnItemClickListener { item, _ ->
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                val row = item as ConversationListRow
                val bundle = bundleOf(
                    PARAM_CONVERSATION_DATABASE_ID to row.conversation.databaseId,
                    PARAM_SENDER_ID to row.conversation.senderUid
                )

                view?.findNavController()?.navigate(
                    R.id.action_conversationsListFragment_to_conversationDetailsFragment,
                    bundle
                )
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }

        companyConversationsAdapter.setOnItemClickListener { item, _ ->
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                val row = item as ConversationListRow
                val bundle = bundleOf(
                    PARAM_CONVERSATION_DATABASE_ID to row.conversation.databaseId,
                    PARAM_SENDER_ID to row.conversation.senderUid
                )

                view?.findNavController()?.navigate(
                    R.id.action_conversationsListFragment_to_conversationDetailsFragment,
                    bundle
                )
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }


        firebaseChatRepository.getUserConversationsListData()
            .observe(viewLifecycleOwner, { conversations ->
                if (conversations != null) {
                    if (conversations.isNotEmpty()) {
                        binding.conversationsEmptyListText.visibility = View.GONE
                        privateConversationsAdapter.clear()
                        companyConversationsAdapter.clear()
                        for (conversation in conversations) {
                            if (conversation.companyMessage == false) {
                                privateConversationsAdapter.add(ConversationListRow(conversation))
                            } else {
                                companyConversationsAdapter.add(ConversationListRow(conversation))
                            }
                        }
                        if (companyConversationsAdapter.itemCount == 0) {
                            binding.conversationsWithCompanyRecyclerViewLayout.visibility =
                                View.GONE
                        } else {
                            binding.conversationsWithCompanyRecyclerViewLayout.visibility =
                                View.VISIBLE
                        }
                        if (privateConversationsAdapter.itemCount == 0) {
                            binding.conversationsRecyclerViewLayout.visibility = View.GONE
                        } else {
                            binding.conversationsRecyclerViewLayout.visibility = View.VISIBLE
                        }
                        binding.conversationsRecyclerView.adapter = privateConversationsAdapter
                        binding.conversationsWithCompanyRecyclerView.adapter =
                            companyConversationsAdapter
                    } else {
                        binding.conversationsWithCompanyRecyclerViewLayout.visibility =
                            View.GONE
                        binding.conversationsRecyclerViewLayout.visibility = View.GONE
                        binding.conversationsEmptyListText.visibility = View.VISIBLE
                    }
                } else {
                    binding.conversationsWithCompanyRecyclerViewLayout.visibility = View.GONE
                    binding.conversationsRecyclerViewLayout.visibility = View.GONE
                    binding.conversationsEmptyListText.visibility = View.VISIBLE
                }
            })



        binding.conversationsStartNewConversation.setOnClickListener {

            firebaseChatRepository.getUsersForConversationListData()
                .observe(viewLifecycleOwner, { usersList ->
                    if (usersList != null && usersList.isNotEmpty()) {
                        usersListAdapter.clear()
                        for (user in usersList) {
                            usersListAdapter.add(UsersListRow(user))
                        }
                        startNewConversationUsersListRecyclerView.adapter = usersListAdapter

                        startNewConversationSearchView.setOnQueryTextListener(object :
                            SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String?): Boolean {
                                if (query != null) {
                                    if (query.isNotEmpty()) {
                                        val selectedList = usersList.filter {
                                            it.userNameSurname!!.contains(query, true)
                                        }
                                        usersListAdapter.clear()
                                        for (user in selectedList) {
                                            usersListAdapter.add(UsersListRow(user))
                                        }
                                        startNewConversationUsersListRecyclerView.adapter = usersListAdapter
                                    } else {
                                        usersListAdapter.clear()
                                        for (user in usersList) {
                                            usersListAdapter.add(UsersListRow(user))
                                        }
                                        startNewConversationUsersListRecyclerView.adapter = usersListAdapter
                                    }
                                }
                                return false
                            }

                            override fun onQueryTextChange(newText: String?): Boolean {
                                if (newText != null) {
                                    if (newText.isNotEmpty()) {
                                        val selectedList = usersList.filter {
                                            it.userNameSurname!!.contains(newText, true)
                                        }
                                        usersListAdapter.clear()
                                        for (user in selectedList) {
                                            usersListAdapter.add(UsersListRow(user))
                                        }
                                        startNewConversationUsersListRecyclerView.adapter = usersListAdapter
                                    } else {
                                        usersListAdapter.clear()
                                        for (user in usersList) {
                                            usersListAdapter.add(UsersListRow(user))
                                        }
                                        startNewConversationUsersListRecyclerView.adapter = usersListAdapter
                                    }
                                }
                                return false
                            }
                        })
                    }
                })

            startNewConversationBottomSheetDialog.show()
        }

    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
        firebaseChatRepository.resetTaskListeners()
    }
}





