package com.google.firebase.goloco.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.type.LatLng

/**
 * Local POJO.
 */
@IgnoreExtraProperties
data class Local(
    var name: String? = null,
    var city: String? = null,
    var category: String? = null,
    var photo: String? = null,
    var lat: Double = 0.toDouble(),
    var lon: Double = 0.toDouble(),
    var numRatings: Int = 0,
    var avgRating: Double = 0.toDouble()
) {

    companion object {

        const val FIELD_CITY = "city"
        const val FIELD_CATEGORY = "category"
        const val FIELD_LAT = "lat"
        const val FIELD_LON = "lon"
        const val FIELD_POPULARITY = "numRatings"
        const val FIELD_AVG_RATING = "avgRating"
    }
}
