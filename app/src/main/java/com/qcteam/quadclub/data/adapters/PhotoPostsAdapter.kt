package com.qcteam.quadclub.data.adapters

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.qcteam.quadclub.R
import com.qcteam.quadclub.data.PhotoPost
import com.qcteam.quadclub.data.PhotoPostChanges
import com.qcteam.quadclub.data.helpers.AlertBuilder


class PhotoPostsAdapter(
    private val postsList: List<PhotoPost>,
    private var changes: MutableList<PhotoPostChanges>
) :
    RecyclerView.Adapter<PhotoPostsAdapter.PhotoPostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoPostViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.photo_post_recycler_card, parent, false)

        return PhotoPostViewHolder(view, changes)
    }

    override fun onBindViewHolder(holder: PhotoPostViewHolder, position: Int) {
        val item = postsList[position]
        holder.bindPhotoPost(item)
    }

    override fun getItemCount(): Int {
        return postsList.size
    }

    class PhotoPostViewHolder(v: View, private val changes: MutableList<PhotoPostChanges>) :
        RecyclerView.ViewHolder(v) {

        val view = v
        lateinit var post: PhotoPost

        private val authorPhoto: ImageView = view.findViewById(R.id.photo_post_author_photo)
        private val authorNameSurname: TextView =
            view.findViewById(R.id.photo_post_author_name_surname_text)
        private val postPhoto: ImageView = view.findViewById(R.id.photo_post_photo)
        private val postLikesNumber: TextView = view.findViewById(R.id.photo_post_like_number)
        private val postTags: TextView = view.findViewById(R.id.photo_post_tags_text)
        private val likeButton: ImageButton = view.findViewById(R.id.photo_post_like_button)
        private val deleteButton: ImageButton = view.findViewById(R.id.delete_post_button)
        private val vehicleTagLayout: ConstraintLayout =
            view.findViewById(R.id.photo_post_vehicle_tag_text_layout)
        private val vehicleTag: TextView = view.findViewById(R.id.photo_post_vehicle_tag_text)


        fun bindPhotoPost(photoPost: PhotoPost) {
            this.post = photoPost

            if(post.authorPhotoUrl?.isNotEmpty() == true) {
                Glide.with(view).load(post.authorPhotoUrl).centerCrop().diskCacheStrategy(
                    DiskCacheStrategy.RESOURCE
                ).into(authorPhoto)
            } else {
                authorPhoto.setImageDrawable(
                    ContextCompat.getDrawable(
                    view.context,
                    R.drawable.ic_account_circle_black
                ))
            }

            if(!post.postPhotoUrl.isNullOrEmpty()) {
                Glide.with(view)
                    .load(post.postPhotoUrl).centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(postPhoto)
            }

            authorNameSurname.text = post.authorNameSurname
            postLikesNumber.text = post.likesCounter.toString()
            postTags.text = post.tags

            if (post.vehicle != null) {
                vehicleTagLayout.visibility = View.VISIBLE
                vehicleTag.text = post.vehicle!!.vehicleModel
            }


            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser!!.uid
            var likedByCurrentUser = false

            if (post.likedBy?.contains(userId) == true) {
                likeButton.setImageResource(R.drawable.ic_thumb_up)
                likedByCurrentUser = true
            } else {
                likeButton.setImageResource(R.drawable.ic_thumb_up_off)
            }

            if (post.authorUid == userId) {
                deleteButton.visibility = View.VISIBLE
            } else {
                deleteButton.visibility = View.GONE
            }

            likeButton.setOnClickListener {
                val connectivityManager =
                    view.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val currentNetwork = connectivityManager.activeNetwork

                if (currentNetwork != null) {
                    if (likedByCurrentUser) {
                        likeButton.setImageResource(R.drawable.ic_thumb_up_off)
                        if (post.likedBy?.contains(userId) == true) {
                            val likes = post.likesCounter - 1
                            postLikesNumber.text = likes.toString()
                        } else {
                            postLikesNumber.text = post.likesCounter.toString()
                        }

                        try {
                            changes.remove(PhotoPostChanges(post.documentId.toString(), 1))
                        } catch (e: Exception) {
                        }
                        changes.add(PhotoPostChanges(post.documentId.toString(), -1))

                        likedByCurrentUser = false
                    } else {
                        likeButton.setImageResource(R.drawable.ic_thumb_up)
                        val likes = post.likesCounter + 1
                        postLikesNumber.text = likes.toString()

                        changes.add(PhotoPostChanges(post.documentId.toString(), 1))
                        try {
                            changes.remove(PhotoPostChanges(post.documentId.toString(), -1))
                        } catch (e: Exception) {
                        }

                        likedByCurrentUser = true
                    }
                } else {
                    AlertBuilder.buildNetworkAlert(view.context)
                }
            }

            deleteButton.setOnClickListener {
                val connectivityManager =
                    view.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val currentNetwork = connectivityManager.activeNetwork

                if (currentNetwork != null) {
                    val db = FirebaseFirestore.getInstance()
                    val storage = FirebaseStorage.getInstance()

                    val photoPath = storage.getReference(post.postPhotoPath.toString())
                    val docRef = db.collection("posts").document(post.documentId.toString())

                    photoPath.delete()
                        .addOnSuccessListener {
                            docRef.delete()
                                .addOnSuccessListener {
                                    val builder = AlertDialog.Builder(view.context)
                                    builder.setTitle("Ususnięto post")
                                    builder.setMessage("Post został pomyślnie usunięty.")
                                    builder.setPositiveButton(android.R.string.ok) { _, _ ->
                                    }
                                    builder.show()
                                }
                                .addOnFailureListener { error ->
                                    AlertBuilder.buildErrorAlert(view.context, error)
                                }
                        }
                        .addOnFailureListener { error ->
                            AlertBuilder.buildErrorAlert(view.context, error)
                        }
                } else {
                    AlertBuilder.buildNetworkAlert(view.context)
                }
            }

        }


    }
}