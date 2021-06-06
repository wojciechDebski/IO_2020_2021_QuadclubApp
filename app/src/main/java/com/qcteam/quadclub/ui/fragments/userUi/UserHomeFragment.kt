package com.qcteam.quadclub.ui.fragments.userUi

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.adapters.VehicleAdapter
import com.qcteam.quadclub.data.enums.FragmentEnums
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_CONTACT
import com.qcteam.quadclub.data.helpers.PARAM_SERVICE
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentUserHomeBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserHomeFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentUserHomeBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository : FirebaseRepository by activityViewModels()

    //Variables responsible for bottom choose service type sheet dialog
    private lateinit var chooseServiceTypeDialog: BottomSheetDialog
    private lateinit var chooseServiceTypeBottomSheetView: View
    private lateinit var chooseServiceTypeServiceCardButton: CardView
    private lateinit var chooseServiceTypeContactCardButton: CardView


    @DelicateCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserHomeBinding.inflate(layoutInflater, container, false)

        setupErrorListener()

        //Inflate choose service type bottom sheet view
        chooseServiceTypeBottomSheetView = layoutInflater.inflate(R.layout.select_servis_action_bottom_sheet, container, false)

        //Handling back press buttons actions
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                activity?.moveTaskToBack(true)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        //Setting up user information in proper fields
        setupUserInformationFields()

        //Setting up recycler view containing user vehicles
        setupHomeVehicleCardsRecyclerView()

        //Setting up cover photo for gallery box
        setupCoverPhotoForGalleryBoxAndFunctionality()

        //Setting up navigation layout boxes and buttons
        setupNavigationBoxesAndButtons()

        //Setting up bottom choose service type sheet dialog
        setupBottomChooseServiceTypeSheetDialog()

        //Setting up bottom choose service type sheet dialog functionality
        setupBottomChooseServiceTypeSheetDialogFunctionality()

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun setupUserInformationFields() {
        firebaseRepository.getLoggedUserInfoData().observe(viewLifecycleOwner, { user ->
            if (user != null) {
                binding.userName.text = user.firstName

                if(user.userProfilePhotoUrl!!.isNotEmpty()) {
                    Glide.with(this).load(user.userProfilePhotoUrl).centerCrop().diskCacheStrategy(
                        DiskCacheStrategy.RESOURCE
                    ).into(binding.userPhoto)
                } else {
                    binding.userPhoto.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_account_circle_black))
                }
            }
        })
    }

    private fun setupHomeVehicleCardsRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(this.context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.homeQuadCardsRecycledView.layoutManager = linearLayoutManager

        firebaseRepository.getUserVehicleListData().observe(viewLifecycleOwner, { vehicleList ->
            if (vehicleList != null && vehicleList.isNotEmpty()) {
                val adapter = VehicleAdapter(vehicleList, FragmentEnums.HOME_FRAGMENT)
                adapter.notifyDataSetChanged()
                binding.homeQuadCardsRecycledView.adapter = adapter

                binding.recyclerQuadCardEmptyInclude.recyclerQuadCardEmpty.visibility =
                    View.GONE
                binding.homeQuadCardsRecycledView.visibility = View.VISIBLE
            } else {
                binding.recyclerQuadCardEmptyInclude.recyclerQuadCardEmpty.visibility =
                    View.VISIBLE
                binding.homeQuadCardsRecycledView.visibility = View.GONE
            }
        })
    }

    private fun setupCoverPhotoForGalleryBoxAndFunctionality() {
        binding.homeGalleryHeaderButton.setOnClickListener {
            val connectivityManager =
                requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                view?.findNavController()
                    ?.navigate(R.id.action_userHomeFragment_to_addPhotoPostFragment)
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }

        firebaseRepository.getPostsListData().observe(viewLifecycleOwner, { postsList ->
            if(postsList != null){
                val newestPostPhotoUrl = postsList[0].postPhotoUrl

                Glide.with(this).load(newestPostPhotoUrl).diskCacheStrategy(
                    DiskCacheStrategy.RESOURCE
                ).into(binding.homeGalleryBoxBodyPhoto)
            }
        })
    }

    private fun setupNavigationBoxesAndButtons() {
        binding.homeVehiclesBoxInclude.homeVehiclesBox.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_userHomeFragment_to_vehiclesFragment)
        }

        binding.homeMyVehiclesHeader.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_userHomeFragment_to_vehiclesFragment)
        }


        binding.userProfileSettings.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_userHomeFragment_to_userProfileFragment)
        }

        binding.homeGalleryHeader.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_userHomeFragment_to_galleryFragment)
        }

        binding.homeGoToGallery.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_userHomeFragment_to_galleryFragment)
        }

        binding.homeRouteBoxInclude.homeRouteBox.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_userHomeFragment_to_routesFragment)
        }

        binding.homeRouteBoxInclude.homeRouteBoxGoButton.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_userHomeFragment_to_trackingFragment)
        }
    }

    private fun setupBottomChooseServiceTypeSheetDialog() {
        chooseServiceTypeServiceCardButton = chooseServiceTypeBottomSheetView.findViewById(R.id.select_action_service)
        chooseServiceTypeContactCardButton = chooseServiceTypeBottomSheetView.findViewById(R.id.select_action_contact)

        chooseServiceTypeDialog = BottomSheetDialog(this.requireContext())
        chooseServiceTypeDialog.setContentView(chooseServiceTypeBottomSheetView)
        chooseServiceTypeDialog.behavior.apply {
            isFitToContents = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun setupBottomChooseServiceTypeSheetDialogFunctionality() {
        binding.homeServicesBoxInclude.homeServicesBox.setOnClickListener {
            chooseServiceTypeDialog.show()
        }

        chooseServiceTypeServiceCardButton.setOnClickListener {
            firebaseRepository.getCompaniesList()

            val bundle = bundleOf(
                PARAM_SERVICE to true,
                PARAM_CONTACT to false
            )
            view?.findNavController()
                ?.navigate(R.id.action_userHomeFragment_to_searchServiceFragment, bundle)
            chooseServiceTypeDialog.dismiss()
        }

        chooseServiceTypeContactCardButton.setOnClickListener {
            firebaseRepository.getCompaniesList()

            val bundle = bundleOf(
                PARAM_SERVICE to false,
                PARAM_CONTACT to true
            )
            view?.findNavController()
                ?.navigate(R.id.action_userHomeFragment_to_searchServiceFragment, bundle)
            chooseServiceTypeDialog.dismiss()
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}