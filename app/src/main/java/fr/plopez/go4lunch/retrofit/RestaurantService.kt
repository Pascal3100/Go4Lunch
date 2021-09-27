package fr.plopez.go4lunch.retrofit

import fr.plopez.go4lunch.data.model.restaurant.DetailsQueryResult
import fr.plopez.go4lunch.data.model.restaurant.NearbyQueryResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RestaurantService {

    companion object {
        private const val NEARBY_SEARCH_REQUEST_PATH = "nearbysearch/json"
        private const val DETAILS_REQUEST_PATH = "details/json"
        private const val PHOTO_REQUEST_PATH = "photo"
    }

    @GET(NEARBY_SEARCH_REQUEST_PATH)
    suspend fun getNearbyRestaurants(
        @Query("key") key : String,
        @Query("type") type : String,
        @Query("location") location : String,
        @Query("radius") radius : String
    ): Response<NearbyQueryResult>

    @GET(DETAILS_REQUEST_PATH)
    suspend fun getOpeningPeriodsForRestaurant(
        @Query("key") key : String,
        @Query("fields") fields : String,
        @Query("place_id") placeId : String
        ): Response<DetailsQueryResult>

    @GET(PHOTO_REQUEST_PATH)
    suspend fun getPhotoForRestaurant(
        @Query("key") key : String,
        @Query("photo_reference") photoReference : String,
        @Query("maxheight") maxHeight : String
        ): String
}