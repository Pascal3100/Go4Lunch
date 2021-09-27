package fr.plopez.go4lunch.data.repositories

import android.util.Log
import fr.plopez.go4lunch.data.RestaurantDAO
import fr.plopez.go4lunch.data.RestaurantsCacheDatabase
import fr.plopez.go4lunch.data.model.restaurant.RestaurantQueryResponseItem
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference
import fr.plopez.go4lunch.di.NearbyParameters
import fr.plopez.go4lunch.retrofit.RestaurantService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RestaurantsRepository @Inject constructor(
    private val restaurantService: RestaurantService,
    private val nearbyParameters: NearbyParameters,
    private val restaurantsCacheDAO: RestaurantDAO
) {

    companion object {
        private const val MAX_HEIGHT = "400"
        private const val PERIODS_SEARCH_FIELD = "opening_hours"
    }


    private val lastRequestTimeStampMutableSharedFlow = MutableSharedFlow<Long>(replay = 1)
    val lastRequestTimeStampSharedFlow = lastRequestTimeStampMutableSharedFlow.asSharedFlow()

    suspend fun getRestaurantsAroundPosition(
        latitude: String,
        longitude: String,
        radius: String,
    ): ResponseStatus {
        try {
            val location = "$latitude,$longitude"
            val response = restaurantService.getNearbyRestaurants(
                nearbyParameters.key,
                nearbyParameters.type,
                location,
                radius
            )

            val responseBody = response.body()
            //
            // ONLY FOR DEV PURPOSE
            if (response.isSuccessful && responseBody != null) {
                buildRestaurantList(responseBody.results, latitude, longitude)
            }
            // --------------------
            //

            return if (response.isSuccessful && responseBody != null) {
                ResponseStatus.Success(responseBody.results)
            } else {
                ResponseStatus.NoResponse
            }

        } catch (e: IOException) {
            return ResponseStatus.StatusError.IOException

        } catch (e: HttpException) {
            return ResponseStatus.StatusError.HttpException
        }
    }

    // Retrieve photo Url per restaurant
    private suspend fun getRestaurantPhotoUrl(photoReference: String): String {
//        try {
//            return restaurantService.getPhotoForRestaurant(
//                nearbyParameters.key,
//                photoReference,
//                MAX_HEIGHT
//            )
//
//        } catch (e: IOException) {
//            return ""
//
//        } catch (e: HttpException) {
//            return ""
//        }
        return restaurantService.getPhotoForRestaurant(
            nearbyParameters.key,
            photoReference,
            MAX_HEIGHT
        )


    }

    // Retrieve opening hours per restaurant
    private suspend fun getRestaurantOpeningPeriods(placeId: String): List<fr.plopez.go4lunch.data.model.restaurant.Period> {

        try {
            val response = restaurantService.getOpeningPeriodsForRestaurant(
                nearbyParameters.key,
                PERIODS_SEARCH_FIELD,
                placeId
            )

            val responseBody = response.body()

            return if (response.isSuccessful && responseBody != null) {
                responseBody.result.opening_hours.periods
            } else {
                emptyList()
            }

        } catch (e: IOException) {
            return emptyList()

        } catch (e: HttpException) {
            return emptyList()
        }
    }

    private suspend fun buildRestaurantList(
        responseBodyResults: List<RestaurantQueryResponseItem>,
        latitude: String,
        longitude: String
    ) {

        // Create the current time stamp
        val timeStamp = System.currentTimeMillis()

        // Update the shared Flow to allow other view Models to request the right restaurants list
        lastRequestTimeStampMutableSharedFlow.emit(timeStamp)

        // Store Request in cache
        restaurantsCacheDAO.insertQuery(
            RestaurantsQuery(
                timeStamp,
                latitude.toDouble(),
                longitude.toDouble()
            )
        )

        Log.d("TAG", "####  responseBodyResults contains ${responseBodyResults.size} items")
        // Store all the attached restaurants

        var photoUrl = ""
        var periodList = emptyList<fr.plopez.go4lunch.data.model.restaurant.Period>()

        responseBodyResults.forEach {

            Log.d("TAG", "#### item = $it: ")

            photoUrl = getRestaurantPhotoUrl(it.photos.first().photoReference)

            Log.d("TAG", "#### photo ref = ${it.photos.first().photoReference}")
            Log.d("TAG", "#### photo url = $photoUrl")

            // Inserting restaurant entity
            restaurantsCacheDAO.insertRestaurant(
                RestaurantEntity(
                    it.placeID,
                    it.name,
                    it.vicinity,
                    photoUrl,
                    (it.rating.toFloat() * 3.0f) / 5.0f
                )
            )

            // Store all periods of opening for a restaurant
            periodList = getRestaurantOpeningPeriods(it.placeID)

            if (periodList.isNotEmpty()) {
                periodList.forEach { period ->
                    val day = period.open.day
                    val openingHour = period.open.time
                    val closingHour = period.close.time
                    val periodId = "$day$openingHour$closingHour"

                    // Inserting Period
                    restaurantsCacheDAO.insertRestaurantOpeningPeriod(
                        RestaurantOpeningPeriod(
                            periodId,
                            LocalTime.of(
                                openingHour.take(2).toInt(),
                                openingHour.takeLast(2).toInt()
                            ).toString(),
                            LocalTime.of(
                                closingHour.take(2).toInt(),
                                closingHour.takeLast(2).toInt()
                            ).toString(),
                            day
                        )
                    )

                    // Inserting restaurant - period cross ref
                    restaurantsCacheDAO.insertRestaurantOpeningPeriodCrossReference(
                        RestaurantOpeningPeriodsCrossReference(
                            it.placeID,
                            periodId
                        )
                    )
                }
            }
        }
    }


    sealed class ResponseStatus {

        object NoResponse : ResponseStatus()

        object NoUpdate : ResponseStatus()

        data class Success(val data: List<RestaurantQueryResponseItem>) : ResponseStatus()

        sealed class StatusError : ResponseStatus() {
            object IOException : StatusError()
            object HttpException : StatusError()
        }

    }
}