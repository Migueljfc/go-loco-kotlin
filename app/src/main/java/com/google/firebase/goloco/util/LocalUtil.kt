package com.google.firebase.goloco.util

import android.content.Context
import com.google.firebase.goloco.R
import com.google.firebase.goloco.model.Local
import java.util.*
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Utilities for Restaurants.
 */
object LocalUtil {

    private const val RESTAURANT_URL_FMT = "https://storage.googleapis.com/firestorequickstarts.appspot.com/food_%d.png"
    private const val CATEGORY_URL = "https://firebasestorage.googleapis.com/v0/b/go-loco-fe08c.appspot.com/o/%s%d.jfif?alt=media&token=af47cfff-b015-4738-bd45-40a67ee73396"

    private const val MAX_IMAGE_NUM = 5

    private val NAME_FIRST_WORDS = arrayOf(
            "Infante", "Big", "Belém", "Cruzeiro", "Estação", "Padrão", "Sé", "Quinta", "Torre")

    private val NAME_SECOND_WORDS = arrayOf(
            "D.Pedro", "Ben", "Nacional", "Jerónicos", "S.Jorge", "Descobrimentos", "Mouros", "Ajuda","Pena")

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
        local.photo = getRandomImageUrl(random,local.category.toString())
        local.lat = minLatitude + (maxLatitude - minLatitude) * random.nextDouble()
        local.lon = minLongitude + (maxLongitude - minLongitude) * random.nextDouble()
        local.numRatings = random.nextInt(20)

        // Note: average rating intentionally not set

        return local
    }





    /**
     * Get price represented as dollar signs.
     */
    fun getCoordinatesString(lat: Double, lon:Double): String {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.UP

        return "(${df.format(lat)},${df.format(lon)})"
    }

    private fun getRandomName(random: Random): String {
        return (getRandomString(NAME_FIRST_WORDS, random) + " " +
                getRandomString(NAME_SECOND_WORDS, random))
    }

    private fun getRandomString(array: Array<String>, random: Random): String {
        val ind = random.nextInt(array.size)
        return array[ind]
    }

    /*private fun getRandomImageUrl2(query: String): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://image-search-api.bingoladen.repl.co/image-search?query=$query")
            .build()

        val response = client.newCall(request).execute()
        Log.d("DEBUG","$response")
        if (response.isSuccessful) {
            Log.d("DEBUG", "Tou aqui")
            val responseString = response.body?.string()
            return URL(responseString).toString()
        } else {
            return "https://storage.googleapis.com/firestorequickstarts.appspot.com/food_9.png"
        }
    }*/
    /**
     * Get a random image.
     */
    private fun getRandomImageUrl(random: Random, category: String): String {
        // Integer between 1 and MAX_IMAGE_NUM (inclusive)
        val id = random.nextInt(MAX_IMAGE_NUM) + 1
        val str = category.lowercase()
        var result = ""
        if (category == "Restaurant")
            result = String.format(Locale.getDefault(), RESTAURANT_URL_FMT, id)
        else{
            result = String.format(Locale.getDefault(), CATEGORY_URL, str,id)
        }
        return result
    }


}
