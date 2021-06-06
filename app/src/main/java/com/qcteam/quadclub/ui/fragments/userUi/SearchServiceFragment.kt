package com.qcteam.quadclub.ui.fragments.userUi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.UserInfo
import com.qcteam.quadclub.data.adapters.ServiceAdapter
import com.qcteam.quadclub.data.enums.SearchParam
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_CONTACT
import com.qcteam.quadclub.data.helpers.PARAM_SERVICE
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentSearchServiceBinding


class SearchServiceFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentSearchServiceBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository : FirebaseRepository by activityViewModels()

    //variables passed by navigation bundle
    private var service: Boolean? = null
    private var contact: Boolean? = null

    //variable holding logged user personal information
    private var userInfo: UserInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            service = it.getBoolean(PARAM_SERVICE)
            contact = it.getBoolean(PARAM_CONTACT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSearchServiceBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListener()

        //getting logged user info
        getLoggedUserPersonalInformation()

        //setting up recycler view with companies list
        setupCompaniesListRecyclerView()

        //Handling back button press
        binding.backViewButton.setOnClickListener {
            firebaseRepository.resetTaskListeners()
            findNavController().navigate(R.id.action_searchServiceFragment_to_userHomeFragment)
        }
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                findNavController().navigate(R.id.action_searchServiceFragment_to_userHomeFragment)
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

    private fun setupCompaniesListRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(this.context)
        binding.serviceListRecyclerView.layoutManager = linearLayoutManager

        firebaseRepository.getCompanyListData().observe(viewLifecycleOwner, { list ->
            if (list!= null && userInfo != null) {
                val adapter = ServiceAdapter(list, service!!, contact!!, userInfo!!)
                adapter.notifyDataSetChanged()
                binding.serviceListRecyclerView.adapter = adapter
            }
        })

        setSearchEngine()
    }

    private fun getLoggedUserPersonalInformation() {
        firebaseRepository.getLoggedUserInfoData().observe(viewLifecycleOwner, { _userInfo ->
            if(_userInfo != null){
                userInfo = _userInfo
            }
        })
    }

    private fun setSearchEngine() {
        binding.serviceListSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchByCategory(query!!)
                searchOnChangeRadioButton(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchByCategory(newText!!)
                searchOnChangeRadioButton(newText)
                return false
            }
        })

    }

    private fun searchOnChangeRadioButton(text: String) {
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (text.isNotEmpty()) {
                when (checkedId) {
                    R.id.company_name -> {
                        firebaseRepository.getCompaniesListSearch(text, SearchParam.NAME)
                    }
                    R.id.company_address -> {
                        firebaseRepository.getCompaniesListSearch(text, SearchParam.ADDRESS)
                    }
                    R.id.company_postal_code -> {
                        firebaseRepository.getCompaniesListSearch(text, SearchParam.POSTAL_CODE)
                    }
                    R.id.company_city -> {
                        firebaseRepository.getCompaniesListSearch(text, SearchParam.CITY)

                    }
                    R.id.company_phone -> {
                        firebaseRepository.getCompaniesListSearch(text, SearchParam.PHONE)
                    }
                }
                firebaseRepository.getCompanyListSearchData().observe(viewLifecycleOwner, { list ->
                    if (list!= null) {
                        val adapter = ServiceAdapter(list, service!!, contact!!, userInfo!!)
                        adapter.notifyDataSetChanged()
                        binding.serviceListRecyclerView.adapter = adapter
                    }
                })
            }
        }
    }

    private fun searchByCategory(text: String){
        if(binding.companyName.isChecked){
            firebaseRepository.getCompaniesListSearch(text, SearchParam.NAME)
        }
        if(binding.companyAddress.isChecked) {
            firebaseRepository.getCompaniesListSearch(text, SearchParam.ADDRESS)
        }
        if(binding.companyPostalCode.isChecked) {
            firebaseRepository.getCompaniesListSearch(text, SearchParam.POSTAL_CODE)
        }
        if(binding.companyCity.isChecked) {
            firebaseRepository.getCompaniesListSearch(text, SearchParam.CITY)
        }
        if(binding.companyPhone.isChecked) {
            firebaseRepository.getCompaniesListSearch(text, SearchParam.PHONE)
        }

        firebaseRepository.getCompanyListSearchData().observe(viewLifecycleOwner, { list ->
            if (list!= null) {
                val adapter = ServiceAdapter(list, service!!, contact!!, userInfo!!)
                adapter.notifyDataSetChanged()
                binding.serviceListRecyclerView.adapter = adapter
            }
        })
    }

    override fun onStop() {
        super.onStop()
        firebaseRepository.resetTaskListeners()
    }

}