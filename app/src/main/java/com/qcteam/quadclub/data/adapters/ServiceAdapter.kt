package com.qcteam.quadclub.data.adapters

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.CompanyInfo
import com.qcteam.quadclub.data.ConversationFF
import com.qcteam.quadclub.data.SingleMessage
import com.qcteam.quadclub.data.UserInfo
import com.qcteam.quadclub.data.helpers.AlertBuilder
import com.qcteam.quadclub.data.helpers.PARAM_CONVERSATION_DATABASE_ID
import com.qcteam.quadclub.data.helpers.PARAM_SENDER_ID
import com.qcteam.quadclub.data.helpers.PARAM_TAX_ID
import com.qcteam.quadclub.data.repository.FirebaseRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ServiceAdapter(
    private val companiesList: List<CompanyInfo>,
    private val service: Boolean,
    private val contact: Boolean,
    private val user: UserInfo
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.recycler_services_list, parent, false)

        return ServiceViewHolder(view, service, contact, user)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val item = companiesList[position]
        holder.bindService(item)
    }

    override fun getItemCount(): Int {
        return companiesList.size
    }

    class ServiceViewHolder(
        v: View,
        private val service: Boolean,
        private val contact: Boolean,
        private val user: UserInfo
    ) : RecyclerView.ViewHolder(v), View.OnClickListener {

        private var view: View = v
        private lateinit var company: CompanyInfo

        private var companyName: TextView = view.findViewById(R.id.service_list_company_name)
        private var companyAddress: TextView = view.findViewById(R.id.service_list_company_adress)
        private var companyPostalCode: TextView = view.findViewById(R.id.service_list_company_postal_code)
        private var companyCity: TextView = view.findViewById(R.id.service_list_company_city)
        private var companyPhone: TextView = view.findViewById(R.id.service_list_company_phone)
        private var chooseCompanyButton: ImageButton = view.findViewById(R.id.choose_service_button)

        init {
            chooseCompanyButton.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val connectivityManager =
                view.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetwork = connectivityManager.activeNetwork

            var bundle : Bundle?
            bundle = bundleOf(
                PARAM_TAX_ID to company.companyTaxIdentificationNumber
            )
            if (service) {
                view.findNavController()
                    .navigate(R.id.action_searchServiceFragment_to_serviceFragment, bundle)
            }
            if (contact) {
                if (currentNetwork != null) {
                    val auth = FirebaseAuth.getInstance()
                    val urls =
                        "https://quadclub-mobileapp-default-rtdb.europe-west1.firebasedatabase.app/"
                    val dbRTF = FirebaseDatabase.getInstance(urls)

                    if (auth.currentUser != null) {
                        val db = FirebaseFirestore.getInstance()
                        val myFirestoreRef = db.collection("users").document(auth.currentUser!!.uid)
                            .collection("conversations")
                            .document("${auth.currentUser!!.uid}_${company.uid}")
                        val companyFirestoreRef =
                            db.collection("serviceCompanies").document(company.uid!!)
                                .collection("conversations")
                                .document("${company.uid}_${auth.currentUser!!.uid}")

                        val conversation = ConversationFF(
                            "${auth.currentUser!!.uid}_${company.uid}",
                            company.uid,
                            company.companyName,
                            company.userInfo!!.userProfilePhotoUrl,
                            false,
                            null,
                            true
                        )

                        myFirestoreRef.set(
                            conversation
                        )

                        companyFirestoreRef.set(
                            ConversationFF(
                                "${auth.currentUser!!.uid}_${company.uid}",
                                auth.currentUser!!.uid,
                                "${user.firstName} ${user.lastName}",
                                user.userProfilePhotoUrl,
                                false,
                                null,
                                true
                            )
                        )

                        val current = LocalDateTime.now()

                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        val formatted = current.format(formatter)

                        dbRTF.reference.child("conversations/${auth.currentUser!!.uid}_${company.uid}")
                            .push()
                            .setValue(
                                SingleMessage(
                                    String.format(view.context.resources.getString(R.string.str_welcome_message_service_conversation_template), user.firstName, company.companyName, company.companyPhoneNumber),
                                    "",
                                    company.uid,
                                    formatted
                                )
                            ).addOnSuccessListener {
                                FirebaseRepository().pushNotificationToService(
                                    String.format(view.context.resources.getString(R.string.str_new_message_notification_template), user.firstName, user.lastName),
                                    company.uid!!
                                )
                                bundle = bundleOf(
                                    PARAM_CONVERSATION_DATABASE_ID to "${auth.currentUser!!.uid}_${company.uid}",
                                    PARAM_SENDER_ID to company.uid
                                )

                                view.findNavController().navigate(
                                    R.id.action_searchServiceFragment_to_conversationDetailsFragment,
                                    bundle
                                )
                            }
                    }
                } else {
                    AlertBuilder.buildNetworkAlert(view.context)
                }
            }
        }

        fun bindService(item: CompanyInfo) {
            company = item

            companyName.text = company.companyName
            companyAddress.text = company.companyAddress
            companyPostalCode.text = company.companyPostalCode
            companyCity.text = company.companyCity
            companyPhone.text = company.companyPhoneNumber
        }


    }
}
