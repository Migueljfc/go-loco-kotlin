package com.google.firebase.goloco

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.goloco.databinding.FragmentLocalDetailBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.goloco.adapter.RatingAdapter
import com.google.firebase.goloco.model.Rating
import com.google.firebase.goloco.model.Local
import com.google.firebase.goloco.util.LocalUtil

class LocalDetailFragment : Fragment(),
    EventListener<DocumentSnapshot>,
    RatingDialogFragment.RatingListener {

    private var ratingDialog: RatingDialogFragment? = null

    private lateinit var binding: FragmentLocalDetailBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var localRef: DocumentReference
    private lateinit var ratingAdapter: RatingAdapter

    private var localRegistration: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLocalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get local ID from extras
        val localId = RestaurantDetailFragmentArgs.fromBundle(
            requireArguments()
        ).keyRestaurantId

        // Initialize Firestore
        firestore = Firebase.firestore

        // Get reference to the restaurant
        restaurantRef = firestore.collection("restaurants").document(restaurantId)

        // Get ratings
        val ratingsQuery = restaurantRef
                .collection("ratings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)

        // RecyclerView
        ratingAdapter = object : RatingAdapter(ratingsQuery) {
            override fun onDataChanged() {
                if (itemCount == 0) {
                    binding.recyclerRatings.visibility = View.GONE
                    binding.viewEmptyRatings.visibility = View.VISIBLE
                } else {
                    binding.recyclerRatings.visibility = View.VISIBLE
                    binding.viewEmptyRatings.visibility = View.GONE
                }
            }
        }
        binding.recyclerRatings.layoutManager = LinearLayoutManager(context)
        binding.recyclerRatings.adapter = ratingAdapter

        ratingDialog = RatingDialogFragment()

        binding.localButtonBack.setOnClickListener { onBackArrowClicked() }
        binding.fabShowRatingDialog.setOnClickListener { onAddRatingClicked() }
    }

    public override fun onStart() {
        super.onStart()

        ratingAdapter.startListening()
        restaurantRegistration = restaurantRef.addSnapshotListener(this)
    }

    public override fun onStop() {
        super.onStop()

        ratingAdapter.stopListening()

        restaurantRegistration?.remove()
        restaurantRegistration = null
    }

    /**
     * Listener for the Restaurant document ([.restaurantRef]).
     */
    override fun onEvent(snapshot: DocumentSnapshot?, e: FirebaseFirestoreException?) {
        if (e != null) {
            Log.w(TAG, "restaurant:onEvent", e)
            return
        }

        snapshot?.let {
            val restaurant = snapshot.toObject<Local>()
            if (restaurant != null) {
                onRestaurantLoaded(restaurant)
            }
        }
    }

    private fun onRestaurantLoaded(local: Local) {
        binding.localName.text = local.name
        binding.localRating.rating = local.avgRating.toFloat()
        binding.localNumRatings.text = getString(R.string.fmt_num_ratings, local.numRatings)
        binding.localCity.text = local.city
        binding.localCategory.text = local.category
        binding.localCoordinates.text = LocalUtil.getCoordinatesString(local.lat,local.lon)

        // Background image
        Glide.with(binding.localImage.context)
                .load(local.photo)
                .into(binding.localImage)
    }

    private fun onBackArrowClicked() {
        requireActivity().onBackPressed()
    }

    private fun onAddRatingClicked() {
        ratingDialog?.show(childFragmentManager, RatingDialogFragment.TAG)
    }

    override fun onRating(rating: Rating) {
        // In a transaction, add the new rating and update the aggregate totals
        addRating(restaurantRef, rating)
                .addOnSuccessListener(requireActivity()) {
                    Log.d(TAG, "Rating added")

                    // Hide keyboard and scroll to top
                    hideKeyboard()
                    binding.recyclerRatings.smoothScrollToPosition(0)
                }
                .addOnFailureListener(requireActivity()) { e ->
                    Log.w(TAG, "Add rating failed", e)

                    // Show failure message and hide keyboard
                    hideKeyboard()
                    Snackbar.make(
                        requireView().findViewById(android.R.id.content), "Failed to add rating",
                            Snackbar.LENGTH_SHORT).show()
                }
    }

    private fun addRating(restaurantRef: DocumentReference, rating: Rating): Task<Void> {
        // Create reference for new rating, for use inside the transaction
        val ratingRef = restaurantRef.collection("ratings").document()

        // In a transaction, add the new rating and update the aggregate totals
        return firestore.runTransaction { transaction ->
            val restaurant = transaction.get(restaurantRef).toObject<Restaurant>()
                ?: throw Exception("Restaurant not found at ${restaurantRef.path}")

            // Compute new number of ratings
            val newNumRatings = restaurant.numRatings + 1

            // Compute new average rating
            val oldRatingTotal = restaurant.avgRating * restaurant.numRatings
            val newAvgRating = (oldRatingTotal + rating.rating) / newNumRatings

            // Set new restaurant info
            restaurant.numRatings = newNumRatings
            restaurant.avgRating = newAvgRating

            // Commit to Firestore
            transaction.set(restaurantRef, restaurant)
            transaction.set(ratingRef, rating)

            null
        }
    }

    private fun hideKeyboard() {
        val view = requireActivity().currentFocus
        if (view != null) {
            (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    companion object {

        private const val TAG = "RestaurantDetail"

        const val KEY_RESTAURANT_ID = "key_restaurant_id"
    }
}