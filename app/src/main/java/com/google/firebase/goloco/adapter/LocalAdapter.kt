package com.google.firebase.goloco.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.goloco.R
import com.google.firebase.goloco.databinding.ItemLocalBinding
import com.google.firebase.goloco.databinding.ItemRestaurantBinding
import com.google.firebase.goloco.model.Local
import com.google.firebase.goloco.util.RestaurantUtil

/**
 * RecyclerView adapter for a list of Restaurants.
 */
open class LocalAdapter(query: Query, private val listener: OnLocalSelectedListener) :
        FirestoreAdapter<LocalAdapter.ViewHolder>(query) {

    interface OnLocalSelectedListener {

        fun onLocalSelected(local: DocumentSnapshot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLocalBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(val binding: ItemLocalBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            snapshot: DocumentSnapshot,
            listener: OnLocalSelectedListener?
        ) {

            val local = snapshot.toObject<Local>()
            if (local == null) {
                return
            }

            val resources = binding.root.resources

            // Load image
            Glide.with(binding.localItemImage.context)
                    .load(local.photo)
                    .into(binding.localItemImage)

            val numRatings: Int = local.numRatings

            binding.localItemName.text = local.name
            binding.localItemRating.rating = local.avgRating.toFloat()
            binding.localItemCity.text = local.city
            binding.localItemCategory.text = local.category
            binding.localItemNumRatings.text = resources.getString(
                    R.string.fmt_num_ratings,
                    numRatings)
            binding.localItemCoordinates.text = RestaurantUtil.getCoordinatesString(local.lat,local.lon)
            // Click listener
            binding.root.setOnClickListener {
                listener?.onLocalSelected(snapshot)
            }
        }
    }
}
