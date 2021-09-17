package fr.plopez.go4lunch.interfaces

import fr.plopez.go4lunch.data.model.restaurant.Restaurant
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RestaurantNearbyService {

    companion object {
        private const val REQUEST_PATH = "nearbysearch/json"
    }

    @GET(REQUEST_PATH)
    suspend fun getNearbyRestaurants(
        @Query("key") key : String,
        @Query("type") type : String,
        @Query("location") location : String,
        @Query("radius") radius : String
    ): Response<List<Restaurant>>
}