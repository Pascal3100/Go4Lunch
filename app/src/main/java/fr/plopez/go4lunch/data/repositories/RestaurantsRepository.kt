package fr.plopez.go4lunch.data.repositories

import fr.plopez.go4lunch.data.model.restaurant.Restaurant
import fr.plopez.go4lunch.di.NearbyParameters
import fr.plopez.go4lunch.interfaces.RestaurantNearbyService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class RestaurantsRepository @Inject constructor(
    private val restaurantNearbyService: RestaurantNearbyService,
    private val nearbyParameters: NearbyParameters
) {
    //private val cache : Map<???, List<Restaurant>> = mapOf()

    suspend fun getRestaurantsAroundPosition(
        latitude: String,
        longitude: String,
        radius: String,
    ): ResponseStatus {

        // check for distance between last and actual request

//        Location.distanceBetween(
//            previousLatitude.toDouble(),
//            previousLongitude.toDouble(),
//            latitude.toDouble(),
//            longitude.toDouble()

        try {
            val location = "$latitude,$longitude"
            val response = restaurantNearbyService.getNearbyRestaurants(
                nearbyParameters.key,
                nearbyParameters.type,
                location,
                radius
            )

            val responseBody = response.body()

            return if (response.isSuccessful && responseBody != null) {
                ResponseStatus.Success(responseBody)
            } else {
                ResponseStatus.NoResponse
            }
        } catch (e: IOException) {
            return ResponseStatus.StatusError.IOException
        } catch (e: HttpException) {
            return ResponseStatus.StatusError.HttpException
        }
    }

    sealed class ResponseStatus {

        object NoResponse : ResponseStatus()

        data class Success(val data: List<Restaurant>) : ResponseStatus()

        sealed class StatusError : ResponseStatus() {
            object IOException : StatusError()
            object HttpException : StatusError()
        }
    }
}