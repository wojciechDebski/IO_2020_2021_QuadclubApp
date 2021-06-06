package com.qcteam.quadclub.ui.fragments.companyUi

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.qcteam.quadclub.data.helpers.AppHelper
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.CompanyInfo
import com.qcteam.quadclub.data.ConversationFF
import com.qcteam.quadclub.data.SingleMessage
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_SENDER_ID
import com.qcteam.quadclub.data.helpers.PARAM_CONVERSATION_DATABASE_ID
import com.qcteam.quadclub.data.repository.FirebaseChatRepositoryForCompany
import com.qcteam.quadclub.data.repository.FirebaseRepositoryForCompany
import com.qcteam.quadclub.databinding.FragmentCompanyConversationDetailBinding
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class CompanyConversationDetailFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentCompanyConversationDetailBinding

    //FirebaseRepositoryForCompany view model variable holder
    private val firebaseRepositoryForCompany: FirebaseRepositoryForCompany by activityViewModels()

    //FirebaseChatRepositoryForCompany view model variable holder
    private val firebaseChatRepositoryForCompany: FirebaseChatRepositoryForCompany by activityViewModels()

    //parameters passed by view navigation bundle
    private var conversationDatabaseId: String? = null
    private var clientId: String? = null

    //Variables holding showing conversation data class object and logged company data class object
    private var currentConversation: ConversationFF? = null
    private var companyInfo: CompanyInfo? = null

    //Variables responsible for Firebase Recycler View with messages
    lateinit var linearLayoutManager: LinearLayoutManager
    private var firebaseDatabaseReference: DatabaseReference? = null
    private var firebaseAdapter: FirebaseRecyclerAdapter<SingleMessage, SingleMessageViewHolder>? =
        null

    //Variable holding bitmap of selected vehicle photo
    private var selectedBitmap: Bitmap? = null

    //Activity Result Launcher - when user select new profile photo it places new photo in editUserDataUserPhoto ImageView and update selectedBitmap variable
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (conversationDatabaseId != null) {
                loadConversationHistory(conversationDatabaseId!!)
            }
            if (uri != null) {
                val source = ImageDecoder.createSource(this.requireContext().contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                //Resizing selected photo ----------------
                val resizedBitmap = AppHelper.resizePhoto(500, bitmap)
                //----------------------------------------

                selectedBitmap = resizedBitmap
                binding.conversationDetailsMessageEditText.text = null
                binding.conversationDetailsMessageEditText.visibility = View.GONE
                binding.conversationDetailsMessageSelectedPhotoLayout.visibility = View.VISIBLE
                binding.conversationDetailsMessageSelectedPhoto.setImageBitmap(selectedBitmap)
                binding.conversationDetailsSendMessageButton.isEnabled = true
                binding.conversationDetailsSendMessageButton.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.blue
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationDatabaseId = it.getString(PARAM_CONVERSATION_DATABASE_ID)
            clientId = it.getString(PARAM_SENDER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCompanyConversationDetailBinding.inflate(layoutInflater, container, false)

        //setting up error listener
        setupErrorListener()

        //setting up variables responsible for Firebase Recycler View with messages
        linearLayoutManager = LinearLayoutManager(this.context)
        linearLayoutManager.stackFromEnd = true
        firebaseDatabaseReference =
            FirebaseDatabase.getInstance("https://quadclub-mobileapp-default-rtdb.europe-west1.firebasedatabase.app/").reference

        if (conversationDatabaseId != null) {
            loadConversationHistory(conversationDatabaseId!!)
        }

        //getting logged company info from Firebase
        geLoggedCompanyInfoAndCurrentConversationInfo()

        //setting up sending button functionality
        setupConversationUiFunctionality()

        //Holding back button press
        binding.backViewButton.setOnClickListener {
            firebaseChatRepositoryForCompany.resetTaskListeners()
            firebaseRepositoryForCompany.resetTaskListeners()
            view?.findNavController()
                ?.navigate(R.id.action_companyConversationDetailFragment_to_companyConversationsListFragment)
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseChatRepositoryForCompany.resetTaskListeners()
                firebaseRepositoryForCompany.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_companyConversationDetailFragment_to_companyConversationsListFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)

        return binding.root
    }

    private fun setupErrorListener() {
        firebaseChatRepositoryForCompany.getErrorMessageData()
            .observe(viewLifecycleOwner, { error ->
                if (error != null) {
                    AlertBuilder.buildErrorAlert(requireContext(), error)
                }
            })

        firebaseRepositoryForCompany.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
    }

    private fun geLoggedCompanyInfoAndCurrentConversationInfo() {
        companyInfo = firebaseRepositoryForCompany.getLoggedCompanyInfoData().value

        currentConversation =
            firebaseChatRepositoryForCompany.getCompanyConversationsListData().value?.find {
                it.senderUid == clientId
            }

        if (currentConversation != null) {
            binding.conversationDetailsSendMessageButton.setOnClickListener {
                val connectivityManager =
                    requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val currentNetwork = connectivityManager.activeNetwork

                if (currentNetwork != null) {
                    if (selectedBitmap == null) {
                        sendMessage()
                    } else {
                        sendPhotoMessage()
                    }
                } else {
                    AlertBuilder.buildNetworkAlert(requireContext())
                }
            }

            binding.conversationDetailsHeaderDeleteButton.setOnClickListener {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Uwaga!")
                builder.setMessage("Czy na pewno czesz usunąć konwersację?")

                builder.setPositiveButton("TAK") { _, _ ->
                    firebaseChatRepositoryForCompany.deleteConversation(currentConversation!!)
                }
                builder.setNegativeButton("NIE") { _, _ -> }

                builder.show()

                firebaseChatRepositoryForCompany.getTaskStatusListenerData()
                    .observe(viewLifecycleOwner, { status ->
                        if (status != null) {
                            if (status) {
                                view?.findNavController()
                                    ?.navigate(R.id.action_companyConversationDetailFragment_to_companyConversationsListFragment)
                            }
                        }
                    })

            }

            binding.conversationDetailsMessageAddPhoto.setOnClickListener {
                choosePhotoFromGallery()
            }

            binding.conversationDetailsMessageSelectedPhotoDelete.setOnClickListener {
                selectedBitmap = null
                binding.conversationDetailsMessageEditText.visibility = View.VISIBLE
                binding.conversationDetailsMessageSelectedPhotoLayout.visibility = View.GONE
            }

            binding.conversationDetailsHeaderNameSurname.text =
                currentConversation!!.senderNameSurname

            if (!currentConversation!!.senderPhotoUrl.isNullOrEmpty()) {
                Picasso.get().load(currentConversation!!.senderPhotoUrl)
                    .into(binding.conversationDetailsHeaderPhoto)
            }
        }
    }


    private fun setupConversationUiFunctionality() {

        binding.conversationDetailsMessageEditText.addTextChangedListener { text ->
            if (text.isNullOrEmpty()) {
                binding.conversationDetailsSendMessageButton.isEnabled = false
                binding.conversationDetailsSendMessageButton.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.light_blue
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.conversationDetailsSendMessageButton.isEnabled = true
                binding.conversationDetailsSendMessageButton.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.blue
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }
    }


    class SingleMessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        lateinit var message: SingleMessage

        private val messageLayout: ConstraintLayout =
            itemView.findViewById(R.id.recycler_message_layout)
        private var partnerLayout: ConstraintLayout = itemView.findViewById(R.id.partner_layout)
        private var partnerPhoto: CircleImageView =
            itemView.findViewById(R.id.message_recycler_partner_photo)
        private var partnerMessageText: TextView =
            itemView.findViewById(R.id.message_recycler_partner_text)
        private var partnerMessagePhoto: ImageView =
            itemView.findViewById(R.id.message_recycler_partner_sent_photo)
        private var myLayout: ConstraintLayout = itemView.findViewById(R.id.my_layout)
        private var myPhoto: CircleImageView = itemView.findViewById(R.id.message_recycler_my_photo)
        private var myMessageText: TextView = itemView.findViewById(R.id.message_recycler_my_text)
        private var myMessagePhoto: ImageView =
            itemView.findViewById(R.id.message_recycler_my_sent_photo)

        fun bind(message: SingleMessage, companyInfo: CompanyInfo?, conversation: ConversationFF?) {
            this.message = message

            if (message.senderUid == "1053") {
                messageLayout.visibility = View.GONE
            } else {
                if (message.senderUid == FirebaseAuth.getInstance().uid) {
                    partnerLayout.visibility = View.GONE

                    if (!message.sentPhotoUrl.isNullOrEmpty()) {
                        myMessageText.visibility = View.GONE
                        myMessagePhoto.visibility = View.VISIBLE


                        Glide.with(itemView)
                            .load(message.sentPhotoUrl)
                            .addListener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    resource?.let {
                                        val height = resource.intrinsicHeight.toFloat()
                                        val width = resource.intrinsicWidth.toFloat()
                                        myMessagePhoto.layoutParams.height = (height * 1.5).toInt()
                                        myMessagePhoto.layoutParams.width = (width * 1.5).toInt()

                                        myMessagePhoto.setImageDrawable(resource)
                                    }
                                    return false
                                }

                            })
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .preload()

                    } else {
                        myMessagePhoto.visibility = View.GONE
                        myMessageText.visibility = View.VISIBLE
                        myMessageText.text = message.text
                    }

                    if (companyInfo != null) {
                        if (!companyInfo.userInfo?.userProfilePhotoUrl.isNullOrEmpty()) {
                            Glide.with(itemView).load(companyInfo.userInfo?.userProfilePhotoUrl)
                                .diskCacheStrategy(
                                    DiskCacheStrategy.RESOURCE
                                ).into(myPhoto)
                        }
                    }
                } else {
                    myLayout.visibility = View.GONE

                    if (!message.sentPhotoUrl.isNullOrEmpty()) {
                        partnerMessageText.visibility = View.GONE
                        partnerMessagePhoto.visibility = View.VISIBLE


                        Glide.with(itemView)
                            .load(message.sentPhotoUrl)
                            .addListener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    resource?.let {
                                        val height = resource.intrinsicHeight.toFloat()
                                        val width = resource.intrinsicWidth.toFloat()
                                        partnerMessagePhoto.layoutParams.height =
                                            (height * 1.5).toInt()
                                        partnerMessagePhoto.layoutParams.width =
                                            (width * 1.5).toInt()

                                        partnerMessagePhoto.setImageDrawable(resource)
                                    }
                                    return false
                                }

                            })
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .preload()

                    } else {
                        partnerMessagePhoto.visibility = View.GONE
                        partnerMessageText.visibility = View.VISIBLE
                        partnerMessageText.text = message.text
                    }

                    if (conversation != null) {
                        if (!conversation.senderPhotoUrl.isNullOrEmpty()) {
                            Glide.with(itemView).load(conversation.senderPhotoUrl)
                                .diskCacheStrategy(
                                    DiskCacheStrategy.RESOURCE
                                ).into(partnerPhoto)
                        }
                    }
                }
            }
        }
    }

    private fun loadConversationHistory(conversationDatabaseId: String) {
        val msgRef = firebaseDatabaseReference!!.child("conversations/$conversationDatabaseId")

        val options = FirebaseRecyclerOptions.Builder<SingleMessage>()
            .setQuery(msgRef, SingleMessage::class.java)
            .build()

        firebaseAdapter =
            object : FirebaseRecyclerAdapter<SingleMessage, SingleMessageViewHolder>(options) {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): SingleMessageViewHolder {
                    val inflater = LayoutInflater.from(parent.context)
                    return SingleMessageViewHolder(
                        inflater.inflate(
                            R.layout.recycler_message,
                            parent,
                            false
                        )
                    )
                }

                override fun onBindViewHolder(
                    holder: SingleMessageViewHolder,
                    position: Int,
                    model: SingleMessage
                ) {
                    holder.bind(model, companyInfo, currentConversation)
                }
            }

        firebaseAdapter!!.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val count = firebaseAdapter!!.itemCount
                val lastVisiblePosition =
                    linearLayoutManager.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                val loading = lastVisiblePosition == -1
                val atBottom =
                    positionStart >= count - 1 && lastVisiblePosition == positionStart - 1
                if (loading || atBottom) {
                    binding.conversationDetailsRecyclerView.scrollToPosition(positionStart)
                }
            }
        })

        binding.conversationDetailsRecyclerView.layoutManager = linearLayoutManager
        binding.conversationDetailsRecyclerView.adapter = firebaseAdapter
    }

    override fun onPause() {
        super.onPause()
        if (firebaseAdapter != null) {
            firebaseAdapter!!.stopListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (conversationDatabaseId != null && firebaseAdapter != null) {
            loadConversationHistory(conversationDatabaseId!!)
            firebaseAdapter!!.startListening()
        }

    }

    override fun onStop() {
        super.onStop()
        firebaseChatRepositoryForCompany.resetTaskListeners()
        firebaseRepositoryForCompany.resetTaskListeners()
    }

    private fun sendMessage() {
        val msg = binding.conversationDetailsMessageEditText.text.toString()
        if (msg.isNotEmpty()) {
            firebaseChatRepositoryForCompany.sendMessage(msg, currentConversation!!)
        }
        binding.conversationDetailsMessageEditText.setText("")
    }

    private fun sendPhotoMessage() {
        if (selectedBitmap != null) {
            firebaseChatRepositoryForCompany.sendPhotoMessage(
                selectedBitmap!!,
                currentConversation!!
            )

            selectedBitmap = null
            binding.conversationDetailsMessageEditText.visibility = View.VISIBLE
            binding.conversationDetailsMessageSelectedPhotoLayout.visibility = View.GONE
        }
        binding.conversationDetailsMessageEditText.setText("")
    }

    private fun choosePhotoFromGallery() {
        getContent.launch("image/*")
    }

}