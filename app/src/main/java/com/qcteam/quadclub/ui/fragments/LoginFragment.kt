package com.qcteam.quadclub.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.repository.FirebaseChatRepository
import com.qcteam.quadclub.data.repository.FirebaseChatRepositoryForCompany
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.data.repository.FirebaseRepositoryForCompany
import com.qcteam.quadclub.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentLoginBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //FirebaseRepositoryForCompany view model variable holder
    private val firebaseRepositoryForCompany : FirebaseRepositoryForCompany by activityViewModels()

    //FirebaseChatRepository view model variable holder
    private val firebaseChatRepository: FirebaseChatRepository by activityViewModels()

    //FirebaseChatRepositoryForCompany view model variable holder
    private val firebaseChatRepositoryForCompany: FirebaseChatRepositoryForCompany by activityViewModels()

    //variable holding if login screen is showing
    private var loginScreen: Boolean = true

    //variables holding task dialogs
    private lateinit var createSendingEmailDialog: AlertDialog
    private lateinit var createLoginProgressDialog: AlertDialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)

        //setting up task dialogs
        setupLoginProgressDialog()
        setupSendingEmailDialog()

        //setting up listener if user is logged in
        setupCheckingIfUserIsLoggedAndStartingGettingDataFromFirebase()

        //setting up ui buttons functionality
        setupUiFunctionality()

        //holding back button press
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                activity?.moveTaskToBack(true)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupUiFunctionality() {
        binding.logInButton.setOnClickListener {
            val email = binding.signInEmailAdress.editText?.text.toString().trim()

            if (loginScreen) {

                binding.signInErrorTextHolder.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                val password = binding.signInPassword.editText?.text.toString().trim()

                var valid = 0
                valid += validateEmail(email)
                valid += validatePassword(password)

                if (valid == 2) {
                    createLoginProgressDialog.show()
                    firebaseRepository.loginUser(email, password)

                    firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { e ->
                        if (e != null) {
                            createLoginProgressDialog.cancel()
                            binding.signInErrorTextHolder.text = e.localizedMessage
                        }
                    })
                }
            } else {
                if (validateEmail(email) == 1) {
                    createSendingEmailDialog.show()
                    firebaseRepository.resetPasswordEmail(email)

                    firebaseRepository.getTaskStatusListenerData().observe(viewLifecycleOwner, { send ->
                        if (send != null && send == true) {
                            changeUI()
                            createSendingEmailDialog.cancel()
                            Toast.makeText(requireContext(), "Email został wysłany", Toast.LENGTH_SHORT).show()
                        }
                    })

                    firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { e ->
                        if (e != null) {
                            createSendingEmailDialog.cancel()
                            binding.signInErrorTextHolder.text = e.localizedMessage
                        }
                    })
                }
            }
        }

        binding.forgotPasswordTextButton.setOnClickListener {
            changeUI()
        }

        binding.loginToRegistrationTextBtn.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun setupCheckingIfUserIsLoggedAndStartingGettingDataFromFirebase() {
        firebaseRepository.getAuthorizedUserData().observe(viewLifecycleOwner, { user ->
            if (user != null) {
                createLoginProgressDialog.cancel()
                firebaseRepository.checkIfCompany()

                firebaseRepository.getIsCompanyUserData().observe(viewLifecycleOwner, { status ->
                    if (status != null) {
                        if (status) {
                            firebaseRepositoryForCompany.getCompanyOrders()
                            firebaseRepositoryForCompany.getLoggedCompanyInfo()
                            firebaseRepositoryForCompany.getNotificationsList()
                            firebaseChatRepositoryForCompany.getConversationsList()

                            view?.findNavController()?.navigate(R.id.action_loginFragment_to_companyHomeFragment)
                        } else {
                            firebaseRepository.getVehicleList()
                            firebaseRepository.getCurrentUserInfo()
                            firebaseRepository.getUserServiceOrdersList()
                            firebaseRepository.getOrderCounter()
                            firebaseRepository.getPostsList()
                            firebaseRepository.getNotificationsList()
                            firebaseChatRepository.getConversationsList()
                            firebaseRepository.getUserRoutesList()

                            view?.findNavController()?.navigate(R.id.action_loginFragment_to_userHomeFragment)
                        }
                    }
                })
            }
        })
    }


    override fun onStart() {
        super.onStart()
        firebaseRepository.checkIfUserIsLoggedIn()
        firebaseRepository.resetTaskListeners()
        firebaseChatRepository.resetTaskListeners()
        firebaseRepositoryForCompany.resetTaskListeners()
        firebaseChatRepositoryForCompany.resetTaskListeners()
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
        firebaseChatRepository.resetTaskListeners()
        firebaseRepositoryForCompany.resetTaskListeners()
        firebaseChatRepositoryForCompany.resetTaskListeners()
    }
    

    private fun setupLoginProgressDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_login_progress, null)
        val alertBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        alertBuilder.setCancelable(false)
        createLoginProgressDialog = alertBuilder.create()
    }

    private fun setupSendingEmailDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sending_reset_link, null)
        val alertBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        alertBuilder.setCancelable(false)
        createSendingEmailDialog = alertBuilder.create()
    }


    private fun validateEmail(email: String): Int {
        return if (email.isEmpty()) {
            binding.signInEmailAdress.error = "pole obowiazkowe"
            0
        } else {
            binding.signInEmailAdress.error = null
            1
        }
    }


    private fun validatePassword(password: String): Int {
        return if (password.isEmpty()) {
            binding.signInPassword.error = "pole obowiazkowe"
            0
        } else {
            binding.signInPassword.error = null
            1
        }
    }

    private fun changeUI() {
        if (loginScreen) {
            binding.signInErrorTextHolder.text = ""
            binding.loginScreenHeaderText.text = resources.getString(R.string.password_resetting)
            binding.forgotPasswordTextButton.text = resources.getString(R.string.sign_in)
            binding.signInPassword.visibility = View.GONE
            binding.loginScreenShortText.visibility = View.VISIBLE
            binding.logInButton.text = resources.getString(R.string.reset_password)
            loginScreen = false
        } else {
            binding.signInErrorTextHolder.text = ""
            binding.loginScreenHeaderText.text = resources.getString(R.string.sign_in)
            binding.forgotPasswordTextButton.text = resources.getString(R.string.forgot_password)
            binding.signInPassword.visibility = View.VISIBLE
            binding.loginScreenShortText.visibility = View.INVISIBLE
            binding.logInButton.text = resources.getString(R.string.log_in)
            loginScreen = true
        }
    }

}