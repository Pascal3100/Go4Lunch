package fr.plopez.go4lunch.data.retrofit

import fr.plopez.go4lunch.data.model.restaurant.AutoCompleteQueryResult
import fr.plopez.go4lunch.data.model.restaurant.DetailsQueryResult
import fr.plopez.go4lunch.data.model.restaurant.NearbyQueryResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RestaurantService {

    companion object {
        private const val NEARBY_SEARCH_REQUEST_PATH = "nearbysearch/json"
        private const val DETAILS_REQUEST_PATH = "details/json"
        private const val AUTOCOMPLETE_REQUEST_PATH = "autocomplete/json"
        private const val DEFAULT_VALUE_FOR_STRICTBOUNDS = "true"
        private const val DEFAULT_VALUE_FOR_TYPES = "establishment"
        private const val DEFAULT_VALUE_FOR_COMPONENTS = "country:fr"
        private const val DEFAULT_VALUE_FOR_TYPE = "restaurant"
        private const val DEFAULT_VALUE_FOR_RADIUS = "1000"
        private const val DEFAULT_VALUE_FOR_SESSION_TOKEN = "vd2kGijeh5Rmvoi7nqZmc9jk"
    }

    @GET(NEARBY_SEARCH_REQUEST_PATH)
    suspend fun getNearbyRestaurants(
        @Query("key") key : String,
        @Query("type") type : String = DEFAULT_VALUE_FOR_TYPE,
        @Query("location") location : String,
        @Query("radius") radius : String = DEFAULT_VALUE_FOR_RADIUS
    ): Response<NearbyQueryResult>

    @GET(DETAILS_REQUEST_PATH)
    suspend fun getDetailsForRestaurant(
        @Query("key") key : String,
        @Query("fields") fields : String,
        @Query("place_id") placeId : String
        ): Response<DetailsQueryResult>

    @GET(AUTOCOMPLETE_REQUEST_PATH)
    suspend fun getRestaurantsFromSearch(
        @Query("key") key : String,
        @Query("input") input : String,
        @Query("components") components : String = DEFAULT_VALUE_FOR_COMPONENTS,
        @Query("location") location : String,
        @Query("radius") radius : String = DEFAULT_VALUE_FOR_RADIUS,
        @Query("sessiontoken") sessionToken : String = DEFAULT_VALUE_FOR_SESSION_TOKEN,
        @Query("strictbounds") strictBounds : String = DEFAULT_VALUE_FOR_STRICTBOUNDS,
        @Query("types") types : String = DEFAULT_VALUE_FOR_TYPES,
    ): Response<AutoCompleteQueryResult>


}