package com.google.firebase.goloco

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var city: String= ""
    private var name: String= ""
    private var category: String= ""
    private lateinit var firestore: FirebaseFirestore
    private lateinit var localRef: DocumentReference
    var localId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localId = LocalDetailFragmentArgs.fromBundle(
            requireArguments()
        ).keyLocalId

        // Initialize Firestore
        firestore = Firebase.firestore

        // Get reference to the local
        localRef = firestore.collection("locals").document(localId)
        localRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null && documentSnapshot.exists()){
                    lat = documentSnapshot.getDouble("lat")!!
                    lng = documentSnapshot.getDouble("lon")!!
                    name = documentSnapshot.getString("name")!!
                    city = documentSnapshot.getString("city")!!
                    category = documentSnapshot.getString("category")!!
                    Log.d("DEBUG", "Lat = $lat Lon = $lng")
                    val marker = Marker(mapView)
                    marker.apply {

                        position = GeoPoint(lat, lng)
                        title = name
                        snippet = "$category, $city"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(marker)
                }else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.map_fragment, container, false)
        mapView = view.findViewById(R.id.mapView)
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        Log.d("DEBUG", "Lat = $lat Lon = $lng ")
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
            controller.setZoom(8.0)
            controller.setCenter(GeoPoint(40.04718420106735, -8.118496792104956))
        }

    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    companion object {

        private const val TAG = "DEBUG"

        const val KEY_LOCAL_ID = "key_local_id"
    }
}
