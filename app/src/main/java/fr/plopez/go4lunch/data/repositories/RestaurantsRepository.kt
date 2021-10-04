package fr.plopez.go4lunch.data.repositories

import fr.plopez.go4lunch.data.RestaurantDAO
import fr.plopez.go4lunch.data.model.restaurant.RestaurantQueryResponseItem
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantQueriesCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.di.NearbyConstants
import fr.plopez.go4lunch.retrofit.RestaurantService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.round

class RestaurantsRepository @Inject constructor(
    private val restaurantService: RestaurantService,
    private val nearbyConstants: NearbyConstants,
    private val restaurantsCacheDAO: RestaurantDAO,
) {

    companion object {
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
    ): Flow<ResponseStatus> = flow {

        // Check if a previous request correspond in the Database
        val restaurantsFromDatabase = restaurantsCacheDAO.getNearestRestaurants(
            latitude.toDouble(),
            longitude.toDouble(),
            MAX_DISPLACEMENT_TOL.toFloat()
        )

        // Check for result in database
        if (restaurantsFromDatabase.isNotEmpty()) {
            // Update the shared Flow to allow other view Models to request the right restaurants list
            lastRequestTimeStampMutableSharedFlow.emit(restaurantsFromDatabase.first().query.queryTimeStamp)
            emit(ResponseStatus.Success(restaurantsFromDatabase.first().restaurantList))
            return@flow
        }

        // If no response of the database, check on google
        try {
            val location = "$latitude,$longitude"
            val response = restaurantService.getNearbyRestaurants(
                nearbyConstants.key,
                nearbyConstants.type,
                location,
                radius
            )

            val responseBody = response.body()

            if (response.isSuccessful && responseBody != null && responseBody.results.isNotEmpty()) {

                // Map query result to domain model
                val restaurantEntityList = mapRestaurantQueryToEntity(responseBody.results)

                // Store restaurants in database in a separate coroutine
                withContext(Dispatchers.IO) {
                    storeInDatabase(restaurantEntityList, latitude, longitude)
                }

                // Emit the list of restaurant to the VM
                emit(ResponseStatus.Success(restaurantEntityList))
            } else {
                emit(ResponseStatus.NoResponse)
            }

        } catch (e: IOException) {
            emit(ResponseStatus.StatusError.IOException)

        } catch (e: HttpException) {
            emit(ResponseStatus.StatusError.HttpException)
        }
    }

    // Retrieve opening hours per restaurant
    private suspend fun getRestaurantOpeningPeriods(placeId: String): List<fr.plopez.go4lunch.data.model.restaurant.Period> {

        try {
            val response = restaurantService.getOpeningPeriodsForRestaurant(
                nearbyConstants.key,
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

            // Inserting restaurant entity
            restaurantsCacheDAO.upsertRestaurant(it)

            // Store all periods of opening for a restaurant
            val periodList = getRestaurantOpeningPeriods(it.restaurantId)

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
                            it.restaurantId,
                            periodId
                        )
                    )
                }
            }
        }

    }

    // Map the query nearby object to domain object
    private fun mapRestaurantQueryToEntity(
        restaurantQueryResponseItemList: List<RestaurantQueryResponseItem>
    ): List<RestaurantEntity> = restaurantQueryResponseItemList.mapNotNull {

        if (it.placeID != null
            && it.name != null
            && it.vicinity != null
            && it.geometry != null
            && it.rating != null
        ) {
            RestaurantEntity(
                it.placeID,
                it.name,
                it.vicinity,
                it.geometry.location.lat,
                it.geometry.location.lng,
                it.photos?.firstOrNull()?.photoReference,
                round((it.rating.toFloat() * 3.0f) / 5.0f)
            )
        } else {
            null
        }
    }

    suspend fun getRestaurantsWithOpeningPeriods(requestedTimeStamp: Long): List<RestaurantWithOpeningPeriods> {
        return if (requestedTimeStamp == 0L) {
            emptyList<RestaurantWithOpeningPeriods>()
        } else {
            restaurantsCacheDAO.getCurrentRestaurants(requestedTimeStamp)
        }
    }

    suspend fun getPositionForTimestamp(timestamp: Long) = restaurantsCacheDAO.getPositionForTimestamp(timestamp)

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