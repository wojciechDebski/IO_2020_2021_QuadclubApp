package com.qcteam.quadclub.ui

import android.app.AlertDialog
import android.content.Context
import android.net.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.data.repository.FirebaseRepositoryForCompany
import com.qcteam.quadclub.databinding.ActivityHostBinding

class HostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHostBinding
    private val firebaseRepository: FirebaseRepository by viewModels()
    private val firebaseRepositoryForCompany: FirebaseRepositoryForCompany by viewModels()

    private lateinit var createUploadingDataDialog: AlertDialog

    var isCompany : Boolean? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupCreatingAccountDialog()

        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val currentNetwork = connectivityManager.activeNetwork

        if(currentNetwork == null) {
            createUploadingDataDialog.show()
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController


        binding.companyBottomNavMenu.setupWithNavController(navController)
        binding.userBottomNavMenu.setupWithNavController(navController)

        firebaseRepository.getIsCompanyUserData().observe(this, { status ->
            if (status != null) {
                isCompany = status
            }
        })

        firebaseRepository.getNotificationsListData().observe(this, { listOfNotifications ->
            if(listOfNotifications != null){
                val notDisplayed = listOfNotifications.filter {
                    it.displayed == false
                }.size
                val badge = binding.userBottomNavMenu.getOrCreateBadge(R.id.userNotificationsFragment)
                if(notDisplayed > 0){
                    badge.isVisible = true
                    badge.number = notDisplayed
                } else {
                    badge.isVisible = false
                }
            }
        })

        firebaseRepositoryForCompany.getNotificationsListData().observe(this, { listOfNotifications ->
            if(listOfNotifications != null){
                val notDisplayed = listOfNotifications.filter {
                    it.displayed == false
                }.size
                val badge = binding.companyBottomNavMenu.getOrCreateBadge(R.id.companyNotificationsFragment)
                if(notDisplayed > 0){
                    badge.isVisible = true
                    badge.number = notDisplayed
                } else {
                    badge.isVisible = false
                }
            }
        })


        navController.addOnDestinationChangedListener { _, nd: NavDestination, _ ->
            when (nd.id) {
                R.id.loginFragment -> {
                    goneAll()
                }
                R.id.registerFragment -> {
                    goneAll()
                }
                R.id.userHomeFragment -> {
                    visibleUserNav()
                }
                R.id.companyHomeFragment -> {
                    visibleCompanyNav()
                }
                R.id.addPhotoPostFragment -> {
                    goneUserNav()
                }
                R.id.trackingFragment -> {
                    goneUserNav()
                }
                R.id.galleryFragment -> {
                    goneUserNav()
                }
                R.id.conversationDetailsFragment -> {
                    goneUserNav()
                }
                R.id.companyConversationsListFragment -> {
                    visibleCompanyNav()
                }
                R.id.companyConversationDetailFragment -> {
                    goneCompanyNav()
                }
                else -> {
                    if(isCompany != null) {
                        if (isCompany == false) {
                            visibleUserNav()
                        } else {
                            visibleCompanyNav()
                        }
                    }
                }
            }
        }
    }

    private fun goneAll() {
        binding.userBottomNavMenu.visibility = View.GONE
        binding.companyBottomNavMenu.visibility = View.GONE
    }

    private fun goneUserNav() {
        binding.userBottomNavMenu.visibility = View.GONE
    }

    private fun visibleUserNav() {
        binding.userBottomNavMenu.visibility = View.VISIBLE
    }

    private fun goneCompanyNav() {
        binding.companyBottomNavMenu.visibility = View.GONE
    }

    private fun visibleCompanyNav() {
        binding.companyBottomNavMenu.visibility = View.VISIBLE
    }

    private fun setupCreatingAccountDialog() {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_netwok_disabled, null)
        val refreshButton = dialogView.findViewById<Button>(R.id.network_status_refresh)
        val alertBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        refreshButton.setOnClickListener {

            val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if(currentNetwork != null && createUploadingDataDialog.isShowing) {
                createUploadingDataDialog.cancel()
            }

        }

        alertBuilder.setCancelable(false)
        createUploadingDataDialog = alertBuilder.create()
    }

}