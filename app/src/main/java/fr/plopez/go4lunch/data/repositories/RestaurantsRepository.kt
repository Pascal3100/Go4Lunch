package fr.plopez.go4lunch.data.repositories

import fr.plopez.go4lunch.data.RestaurantDAO
import fr.plopez.go4lunch.data.model.restaurant.RestaurantQueryResponseItem
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantQueriesCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.di.NearbyParameters
import fr.plopez.go4lunch.retrofit.RestaurantService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalTime
import java.util.*
import javax.inject.Inject
import kotlin.math.round

class RestaurantsRepository @Inject constructor(
    private val restaurantService: RestaurantService,
    private val nearbyParameters: NearbyParameters,
    private val restaurantsCacheDAO: RestaurantDAO,
) {

    companion object {
        private const val MAX_WIDTH = "1080"

        // this value is calculated for a tol of 50 m
        private const val MAX_DISPLACEMENT_TOL = "0.000449"

        private const val PERIODS_SEARCH_FIELD = "opening_hours"
    }

    // Store the last requested time stamp for others view models get the correct list of restaurants
    private val lastRequestTimeStampMutableSharedFlow = MutableSharedFlow<Long>(replay = 1)
    val lastRequestTimeStampSharedFlow = lastRequestTimeStampMutableSharedFlow.asSharedFlow()

    suspend fun getRestaurantsAroundPosition(
        latitude: String,
        longitude: String,
        radius: String,
    ): ResponseStatus {

        // Check if a previous request correspond in the Database
        val restaurantsFromDatabase = restaurantsCacheDAO.getNearestRestaurants(
            latitude.toDouble(),
            longitude.toDouble(),
            MAX_DISPLACEMENT_TOL.toFloat()
        )

        // Check for result in database
        if (restaurantsFromDatabase != null) {
            // Update the shared Flow to allow other view Models to request the right restaurants list
            lastRequestTimeStampMutableSharedFlow.emit(restaurantsFromDatabase.query.queryTimeStamp)
            return ResponseStatus.Success(restaurantsFromDatabase.restaurantList)
        }

        // If no response of the database, check on google
        try {
            val location = "$latitude,$longitude"
            val response = restaurantService.getNearbyRestaurants(
                nearbyParameters.key,
                nearbyParameters.type,
                location,
                radius
            )

            val responseBody = response.body()

            when {
                response.isSuccessful && responseBody != null && responseBody.results.isNotEmpty() -> {

                    val restaurantEntityList = mapRestaurantQueryToEntity(responseBody.results)

                    storeInDatabase(restaurantEntityList, latitude, longitude)

                    return ResponseStatus.Success(restaurantEntityList)
                }
                else -> {
                    return ResponseStatus.NoResponse
                }
            }

        } catch (e: IOException) {
            return ResponseStatus.StatusError.IOException

        } catch (e: HttpException) {
            return ResponseStatus.StatusError.HttpException
        }
    }

    // Retrieve photo Url per restaurant
    private suspend fun mapRestaurantPhotoUrl(photoReference: String): String {
        return "https://maps.googleapis.com/maps/api/place/photo?" +
                "maxwidth=$MAX_WIDTH&" +
                "photoreference=$photoReference&" +
                "key=${nearbyParameters.key};"
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

    private suspend fun storeInDatabase(
        restaurantEntityList: List<RestaurantEntity>,
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

        // Store all the attached restaurants
        restaurantEntityList.forEach {

            // Inserting restaurant queries cross ref
            restaurantsCacheDAO.insertRestaurantQueriesCrossReference(
                RestaurantQueriesCrossReference(
                    timeStamp,
                    it.restaurantId
                )
            )

            if (!restaurantsCacheDAO.isRestaurantExist(it.restaurantId)) {

                // Inserting restaurant entity
                restaurantsCacheDAO.insertRestaurant(it)

                // Store all periods of opening for a restaurant
                val periodList = getRestaurantOpeningPeriods(it.restaurantId)

                if (periodList.isNotEmpty()) {
                    periodList.forEach { period ->

                        val day = period.open.day
                        val openingHour = period.open.time
                        val closingHour = period.close.time
                        val periodId = "$day$openingHour$closingHour"

                        if (!restaurantsCacheDAO.isPeriodExist(periodId)) {

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
                        }

                        // Inserting restaurant - period cross ref
                        restaurantsCacheDAO.insertRestaurantOpeningPeriodCrossReference(
                            RestaurantOpeningPeriodsCrossReference(
                                it.restaurantId,
                                periodId
                            )
                        )
                    }
                }
            }
        }

    }

    private fun mapRestaurantQueryToEntity(
        restaurantQueryResponseItemList: List<RestaurantQueryResponseItem>
    ): List<RestaurantEntity> {

        val restaurantEntityList = mutableListOf<RestaurantEntity>()

        restaurantQueryResponseItemList.forEach {
            val rate = round((it.rating.toFloat() * 3.0f) / 5.0f)
            var photoUrl: String
            if (it.photos == null) {
                photoUrl = ""
            } else {
                photoUrl = it.photos.first().photoReference
            }

            restaurantEntityList.add(
                RestaurantEntity(
                    it.placeID,
                    it.name,
                    it.vicinity,
                    it.geometry.location.lat,
                    it.geometry.location.lng,
                    photoUrl,
                    rate
                )
            )
        }

        return Collections.unmodifiableList(restaurantEntityList)
    }

    suspend fun getRestaurantsWithOpeningPeriods(requestedTimeStamp: Long): List<RestaurantWithOpeningPeriods> {
        return if (requestedTimeStamp == 0L) {
            emptyList<RestaurantWithOpeningPeriods>()
        } else {
            restaurantsCacheDAO.getCurrentRestaurants(requestedTimeStamp)
        }
    }


    sealed class ResponseStatus {

        object NoResponse : ResponseStatus()

        object NoUpdate : ResponseStatus()

        data class Success(val data: List<RestaurantEntity>) : ResponseStatus()

        sealed class StatusError : ResponseStatus() {
            object IOException : StatusError()
            object HttpException : StatusError()
        }

    }
}