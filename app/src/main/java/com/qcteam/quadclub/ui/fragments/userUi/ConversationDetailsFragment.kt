package com.qcteam.quadclub.ui.fragments.userUi

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.ConversationFF
import com.qcteam.quadclub.data.SingleMessage
import com.qcteam.quadclub.data.UserInfo
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.AppHelper
import com.qcteam.quadclub.data.helpers.PARAM_CONVERSATION_DATABASE_ID
import com.qcteam.quadclub.data.helpers.PARAM_SENDER_ID
import com.qcteam.quadclub.data.repository.FirebaseChatRepository
import com.qcteam.quadclub.data.repository.FirebaseRepository
import com.qcteam.quadclub.databinding.FragmentConversationDetailsBinding
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


class ConversationDetailsFragment : Fragment() {

    //binding view variable holder
    private lateinit var binding: FragmentConversationDetailsBinding

    //FirebaseRepository view model variable holder
    private val firebaseRepository: FirebaseRepository by activityViewModels()

    //FirebaseChatRepository view model variable holder
    private val firebaseChatRepository: FirebaseChatRepository by activityViewModels()

    //parameters passed by view navigation bundle
    private var conversationDatabaseId: String? = null
    private var senderId: String? = null

    //Variables holding showing conversation data class object and logged company data class object
    private var currentConversation: ConversationFF? = null
    private var userInfo: UserInfo? = null

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
            senderId = it.getString(PARAM_SENDER_ID)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentConversationDetailsBinding.inflate(layoutInflater, container, false)

        setupErrorListener()

        //setting up variables responsible for Firebase Recycler View with messages
        linearLayoutManager = LinearLayoutManager(this.context)
        linearLayoutManager.stackFromEnd = true
        firebaseDatabaseReference =
            FirebaseDatabase.getInstance("https://quadclub-mobileapp-default-rtdb.europe-west1.firebasedatabase.app/").reference

        if (conversationDatabaseId != null) {
            loadConversationHistory(conversationDatabaseId!!)
        }

        //getting logged user info from Firebase
        geLoggedUserInfoAndCurrentConversationInfo()

        //setting up sending button functionality
        setupConversationUiFunctionality()

        binding.backViewButton.setOnClickListener {
            if (currentConversation != null) {
                firebaseChatRepository.updateLastMessage(currentConversation!!)
            }
            firebaseRepository.resetTaskListeners()
            firebaseChatRepository.resetTaskListeners()
            view?.findNavController()
                ?.navigate(R.id.action_conversationDetailsFragment_to_userConversationsListFragment)
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseRepository.resetTaskListeners()
                firebaseChatRepository.resetTaskListeners()
                view?.findNavController()
                    ?.navigate(R.id.action_conversationDetailsFragment_to_userConversationsListFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(callback)


        return binding.root
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

    private fun geLoggedUserInfoAndCurrentConversationInfo() {
        userInfo = firebaseRepository.getLoggedUserInfoData().value

        currentConversation = firebaseChatRepository.getUserConversationsListData().value?.find {
            it.senderUid == senderId
        }

        if (currentConversation != null) {
            binding.conversationDetailsSendMessageButton.setOnClickListener {
                if (selectedBitmap == null) {
                    sendMessage()
                } else {
                    sendPhotoMessage()
                }
            }

            binding.conversationDetailsHeaderDeleteButton.setOnClickListener {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Uwaga!")
                builder.setMessage("Czy na pewno czesz usunąć konwersację?")

                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    firebaseChatRepository.deleteConversation(
                        conversationDatabaseId!!,
                        currentConversation!!
                    )
                }
                builder.setNegativeButton(android.R.string.cancel) { _, _ -> }

                builder.show()

                firebaseChatRepository.getTaskStatusListenerData()
                    .observe(viewLifecycleOwner, { status ->
                        if (status != null) {
                            if (status) {
                                view?.findNavController()
                                    ?.navigate(R.id.action_conversationDetailsFragment_to_userConversationsListFragment)
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
                Picasso.get().load(this.currentConversation!!.senderPhotoUrl)
                    .into(binding.conversationDetailsHeaderPhoto)
            }
        }
    }

    private fun setupErrorListener() {
        firebaseChatRepository.getErrorMessageData()
            .observe(viewLifecycleOwner, { error ->
                if (error != null) {
                    AlertBuilder.buildErrorAlert(requireContext(), error)
                }
            })

        firebaseRepository.getErrorMessageData().observe(viewLifecycleOwner, { error ->
            if (error != null) {
                AlertBuilder.buildErrorAlert(requireContext(), error)
            }
        })
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

        fun bind(message: SingleMessage, userInfo: UserInfo?, conversation: ConversationFF?) {
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
                                    myMessageText.visibility = View.VISIBLE
                                    myMessagePhoto.visibility = View.GONE
                                    partnerMessageText.text = "zdjęcie"
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
                                        //use these sizes whatever needed for
                                        myMessagePhoto.setImageDrawable(resource)
                                    }
                                    return false
                                }

                            })
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .preload()
                    } else {
                        myMessageText.text = message.text
                    }

                    if (userInfo != null) {
                        if (!userInfo.userProfilePhotoUrl.isNullOrEmpty()) {
                            Glide.with(itemView).load(userInfo.userProfilePhotoUrl)
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
                                    partnerMessageText.visibility = View.VISIBLE
                                    partnerMessagePhoto.visibility = View.GONE
                                    partnerMessageText.text = "zdjęcie"
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
                                        //use these sizes whatever needed for
                                        partnerMessagePhoto.setImageDrawable(resource)
                                    }
                                    return false
                                }

                            })
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .preload()
                    } else {
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
                    holder.bind(model, userInfo, currentConversation)
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
        firebaseRepository.resetTaskListeners()
        firebaseChatRepository.resetTaskListeners()
    }


    private fun sendMessage() {
        val msg = binding.conversationDetailsMessageEditText.text.toString()
        if (msg.isNotEmpty()) {
            firebaseChatRepository.sendMessage(msg, currentConversation, userInfo!!)
        }
        binding.conversationDetailsMessageEditText.setText("")
    }


    private fun sendPhotoMessage() {
        if (selectedBitmap != null) {
            firebaseChatRepository.sendPhotoMessage(
                selectedBitmap!!,
                currentConversation,
                userInfo!!
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

