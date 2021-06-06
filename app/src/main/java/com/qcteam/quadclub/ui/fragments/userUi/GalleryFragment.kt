package com.qcteam.quadclub.ui.fragments.userUi

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.PhotoPostChanges
import com.qcteam.quadclub.data.adapters.PhotoPostsAdapter
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentGalleryBinding
import kotlinx.coroutines.DelicateCoroutinesApi


class GalleryFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentGalleryBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //variable holding post like changes
    val photoPostChanges = mutableListOf<PhotoPostChanges>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentGalleryBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListeners()

        //setting up ui buttons click listeners and their functionality
        setupUiFunctionality()

        //setting up gallery posts recycler view
        setupGalleryPostRecyclerView()

        //handling back button press
        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            view?.findNavController()?.navigate(R.id.action_galleryFragment_to_userHomeFragment)
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

    private fun setupGalleryPostRecyclerView() {
        setSearchEngine()

        binding.galleryRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        firebaseRepository.getPostsListData().observe(viewLifecycleOwner, { postsList ->
            if (postsList != null) {
                val adapter = PhotoPostsAdapter(postsList, photoPostChanges)
                adapter.notifyDataSetChanged()
                binding.galleryRecyclerView.adapter = adapter


                binding.galleryRadioGroup.setOnCheckedChangeListener { _, checkedId ->

                    if (photoPostChanges.isNotEmpty()) {
                        firebaseRepository.updatePosts(photoPostChanges)
                        photoPostChanges.clear()
                    }

                    when (checkedId) {
                        R.id.gallery_all -> {
                            binding.galleryRecyclerView.adapter =
                                PhotoPostsAdapter(postsList, photoPostChanges)
                        }
                        R.id.gallery_my_posts -> {
                            val myPosts = postsList.filter {
                                it.authorUid == firebaseRepository.getAuthorizedUserData().value?.uid.toString()
                            }
                            binding.galleryRecyclerView.adapter =
                                PhotoPostsAdapter(myPosts, photoPostChanges)
                        }
                    }
                }
            }
        })
    }

    private fun setupUiFunctionality() {
        binding.addPostPhotoButton.setOnClickListener {
            val connectivityManager =
                requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            if (currentNetwork != null) {
                view?.findNavController()
                    ?.navigate(R.id.action_galleryFragment_to_addPhotoPostFragment)
            } else {
                AlertBuilder.buildNetworkAlert(requireContext())
            }
        }
    }

    private fun setupErrorListeners() {
        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if(error != null){
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun setSearchEngine() {
        binding.gallerySearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (photoPostChanges.isNotEmpty()) {
                    firebaseRepository.updatePosts(photoPostChanges)
                    photoPostChanges.clear()
                }
                searchByTags(query!!)
                searchOnChangeRadioButton(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                firebaseRepository.getPostSearchList(newText)
                if (photoPostChanges.isNotEmpty()) {
                    firebaseRepository.updatePosts(photoPostChanges)
                    photoPostChanges.clear()
                }
                searchByTags(newText!!)
                searchOnChangeRadioButton(newText)
                return false
            }
        })


    }

    private fun searchByTags(text: String) {
        firebaseRepository.getPostSearchList(text)

        when (binding.galleryRadioGroup.checkedRadioButtonId) {
            R.id.gallery_all -> {
                firebaseRepository.getPostListSearchData()
                    .observe(viewLifecycleOwner, { postSearchList ->
                        if (postSearchList != null) {
                            val adapter = PhotoPostsAdapter(postSearchList, photoPostChanges)
                            adapter.notifyDataSetChanged()
                            binding.galleryRecyclerView.adapter = adapter
                        }
                    })
            }
            R.id.gallery_my_posts -> {
                firebaseRepository.getPostListSearchData()
                    .observe(viewLifecycleOwner, { postSearchList ->
                        if (postSearchList != null) {
                            val myPosts = postSearchList.filter {
                                it.authorUid == firebaseRepository.getAuthorizedUserData().value?.uid.toString()
                            }
                            val adapter = PhotoPostsAdapter(myPosts, photoPostChanges)
                            adapter.notifyDataSetChanged()
                            binding.galleryRecyclerView.adapter = adapter
                        }
                    })
            }
        }
    }

    private fun searchOnChangeRadioButton(text: String) {
        binding.galleryRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (photoPostChanges.isNotEmpty()) {
                firebaseRepository.updatePosts(photoPostChanges)
                photoPostChanges.clear()
            }
            println(photoPostChanges.toString())
            when (checkedId) {
                R.id.gallery_all -> {
                    firebaseRepository.getPostSearchList(text)

                    firebaseRepository.getPostListSearchData()
                        .observe(viewLifecycleOwner, { postSearchList ->
                            if (postSearchList != null) {
                                val adapter = PhotoPostsAdapter(postSearchList, photoPostChanges)
                                adapter.notifyDataSetChanged()
                                binding.galleryRecyclerView.adapter = adapter
                            }
                        })
                }
                R.id.gallery_my_posts -> {
                    firebaseRepository.getPostSearchList(text)

                    firebaseRepository.getPostListSearchData()
                        .observe(viewLifecycleOwner, { postSearchList ->
                            if (postSearchList != null) {
                                val myPosts = postSearchList.filter {
                                    it.authorUid == firebaseRepository.getAuthorizedUserData().value?.uid.toString()
                                }
                                val adapter = PhotoPostsAdapter(myPosts, photoPostChanges)
                                adapter.notifyDataSetChanged()
                                binding.galleryRecyclerView.adapter = adapter
                            }
                        })
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (photoPostChanges.isNotEmpty()) {
            firebaseRepository.updatePosts(photoPostChanges)
        }
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }
}