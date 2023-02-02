package com.google.firebase.goloco

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.goloco.adapter.LocalAdapter
import com.google.firebase.goloco.databinding.FragmentMainBinding
import com.google.firebase.goloco.model.Local
import com.google.firebase.goloco.util.LocalUtil
import com.google.firebase.goloco.viewmodel.MainActivityViewModel
import com.google.firebase.ktx.Firebase

class MainFragment : Fragment(),
    FilterDialogFragment.FilterListener,
    LocalAdapter.OnLocalSelectedListener {

    lateinit var firestore: FirebaseFirestore
    private var query: Query? = null

    private lateinit var binding: FragmentMainBinding
    private lateinit var filterDialog: FilterDialogFragment
    private var adapter: LocalAdapter? = null
    private  var location: Location? = null
    private lateinit var viewModel: MainActivityViewModel
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result -> this.onSignInResult(result) }

    private val locationPermissionRequestCode = 1
    private val fineLocationPermission = ACCESS_FINE_LOCATION

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Firestore
        firestore = Firebase.firestore

        // Get the 50 highest rated locals
        query = firestore.collection("locals")
            .orderBy("avgRating", Query.Direction.DESCENDING)
            .limit(LIMIT.toLong())

        // View model
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)

        // Firestore
        firestore = Firebase.firestore

        // RecyclerView
        query?.let {
            adapter = object : LocalAdapter(it, this@MainFragment) {
                override fun onDataChanged() {
                    // Show/hide content if the query returns empty.
                    if (itemCount == 0) {
                        binding.recyclerLocals.visibility = View.GONE
                        binding.viewEmpty.visibility = View.VISIBLE
                    } else {
                        binding.recyclerLocals.visibility = View.VISIBLE
                        binding.viewEmpty.visibility = View.GONE
                    }
                }

                override fun onError(e: FirebaseFirestoreException) {
                    // Show a snackbar on errors
                    Snackbar.make(
                        binding.root,
                        "Error: check logs for info.", Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            binding.recyclerLocals.adapter = adapter
        }

        binding.recyclerLocals.layoutManager = LinearLayoutManager(context)

        // Filter Dialog
        filterDialog = FilterDialogFragment()

        binding.filterBar.setOnClickListener { onFilterClicked() }
        binding.buttonClearFilter.setOnClickListener { onClearFilterClicked() }
        checkLocationPermission()
    }

    override fun onStart() {
        super.onStart()

        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn()
            return
        }

        // Apply filters
        onFilter(viewModel.filters)

        // Start listening for Firestore updates
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //R.id.menu_add_items -> onAddItemsClicked()
            R.id.menu_sign_out -> {
                AuthUI.getInstance().signOut(requireContext())
                startSignIn()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        viewModel.isSigningIn = false

        if (result.resultCode != Activity.RESULT_OK) {
            if (response == null) {
                // User pressed the back button.
                requireActivity().finish()
            } else if (response.error != null && response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                showSignInErrorDialog(R.string.message_no_network)
            } else {
                showSignInErrorDialog(R.string.message_unknown)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun onFilterClicked() {
        // Show the dialog containing filter options
        checkLocationPermission()
        filterDialog.show(childFragmentManager, FilterDialogFragment.TAG)
    }

    private fun onClearFilterClicked() {
        filterDialog.resetFilters()

        onFilter(Filters.default)
    }

    override fun onLocalSelected(local: DocumentSnapshot) {
        // Go to the details page for the selected local
        val action = MainFragmentDirections
            .actionMainFragmentToLocalDetailFragment(local.id)
        findNavController().navigate(action)
    }

    override fun onFilter(filters: Filters) {
        // Construct query basic query
        var query: Query = firestore.collection("locals")

        // Category (equality filter)
        if (filters.hasCategory()) {
            query = query.whereEqualTo(Local.FIELD_CATEGORY, filters.category)
        }

        // City (equality filter)
        if (filters.hasCity()) {
            query = query.whereEqualTo(Local.FIELD_CITY, filters.city)
        }

        // Price (equality filter)
        if (filters.hasPrice()) {
            query = query.whereEqualTo(Local.FIELD_LAT, filters.price)
        }

        // Sort by (orderBy with direction)
        if (filters.hasSortBy()) {
            query = query.orderBy(filters.sortBy.toString(), filters.sortDirection)
        }

        // Limit items
        query = query.limit(LIMIT.toLong())

        // Update the query
        adapter?.setQuery(query)

        // Set header
        binding.textCurrentSearch.text = HtmlCompat.fromHtml(
            filters.getSearchDescription(requireContext()),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.textCurrentSortBy.text = filters.getOrderDescription(requireContext())

        // Save filters
        viewModel.filters = filters
    }

    private fun shouldStartSignIn(): Boolean {
        return !viewModel.isSigningIn && Firebase.auth.currentUser == null
    }

    private fun startSignIn() {
        // Sign in with FirebaseUI
        val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(listOf(AuthUI.IdpConfig.EmailBuilder().build()))
                .setIsSmartLockEnabled(false)
                .build()

        signInLauncher.launch(intent)
        viewModel.isSigningIn = true
    }

    private fun onAddItemsClicked() {
        Log.d("OAIC", "onAddItemsClicked")
        val localsRef = firestore.collection("locals")
        for (i in 0..29) {
            // Create random local / ratings
            val randomLocal = LocalUtil.getRandom(requireContext())

            // Add local
            localsRef.add(randomLocal)
        }
    }

    private fun showSignInErrorDialog(@StringRes message: Int) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_sign_in_error)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.option_retry) { _, _ -> startSignIn() }
            .setNegativeButton(R.string.option_exit) { _, _ -> requireActivity().finish() }.create()

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), fineLocationPermission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, fineLocationPermission)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(requireContext())
                    .setTitle("Localization required")
                    .setMessage("We need your location to show you the best places to visit")
                    .setPositiveButton("OK") { dialog, which ->
                        ActivityCompat.requestPermissions(
                            activity as Activity,
                            arrayOf(fineLocationPermission),
                            locationPermissionRequestCode
                        )
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    activity as Activity,
                    arrayOf(fineLocationPermission),
                    locationPermissionRequestCode
                )
            }
        } else {
            // Permission has already been granted
            getUserLocation()
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                getUserLocation()
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(requireContext(), "No permission to access the user location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getUserLocation() {
        val locationManager = requireContext().getSystemService(LocationManager::class.java)
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    }

    private fun showTodoToast() {
        Toast.makeText(context, "TODO: Implement", Toast.LENGTH_SHORT).show()
    }

    companion object {

        private const val TAG = "MainActivity"

        private const val LIMIT = 50
    }
}
