package com.google.firebase.goloco.util

import android.content.Context
import android.util.Log
import com.google.firebase.goloco.R
import com.google.firebase.goloco.model.Local
import java.util.*

/**
 * Utilities for Restaurants.
 */
object RestaurantUtil {

    private const val RESTAURANT_URL_FMT = "https://storage.googleapis.com/firestorequickstarts.appspot.com/food_%d.png"
    private const val MAX_IMAGE_NUM = 22

    private val NAME_FIRST_WORDS = arrayOf(
            "Infante D.Pedro", "Laço", "Misericórdia", "Cruzeiro", "Musica", "D. Afonso Henriques", "Marnoto", "Guerra", "Aveiro")

    private val NAME_SECOND_WORDS = arrayOf(
            "Restaurant", "Cafe", "Monument", "Statue", "Park", "Garden", "Bridge", "Church","Museum")

    /**
     * Create a random Restaurant POJO.
     */
    fun getRandom(context: Context): Local {
        val local = Local()
        val random = Random()
        val minLatitude = 36.9761888
        val maxLatitude = 42.152888
        val minLongitude = -9.5559071
        val maxLongitude = -6.1719288

        // Cities (first elemnt is 'Any')
        var cities = context.resources.getStringArray(R.array.cities)
        cities = Arrays.copyOfRange(cities, 1, cities.size)

        // Categories (first element is 'Any')
        var categories = context.resources.getStringArray(R.array.categories)
        categories = Arrays.copyOfRange(categories, 1, categories.size)


        local.name = getRandomName(random)
        local.city = getRandomString(cities, random)
        local.category = getRandomString(categories, random)
        local.photo = getRandomImageUrl(random)
        local.lat = minLatitude + (maxLatitude - minLatitude) * random.nextDouble()
        local.lon = minLongitude + (maxLongitude - minLongitude) * random.nextDouble()
        local.numRatings = random.nextInt(20)

        // Note: average rating intentionally not set

        return local
    }

    /**
     * Get a random image.
     */
    private fun getRandomImageUrl(random: Random): String {
        // Integer between 1 and MAX_IMAGE_NUM (inclusive)
        val id = random.nextInt(MAX_IMAGE_NUM) + 1
        Log.d("IMAGE URL", "URL DA IMAGEM = " + String.format(Locale.getDefault(), RESTAURANT_URL_FMT, id))
        return String.format(Locale.getDefault(), RESTAURANT_URL_FMT, id)
    }



    /**
     * Get price represented as dollar signs.
     */
    fun getCoordinatesString(lat: Double, lon:Double): String {
        return "($lat,$lon)"
    }

    private fun getRandomName(random: Random): String {
        return (getRandomString(NAME_FIRST_WORDS, random) + " " +
                getRandomString(NAME_SECOND_WORDS, random))
    }

    private fun getRandomString(array: Array<String>, random: Random): String {
        val ind = random.nextInt(array.size)
        return array[ind]
    }


}
