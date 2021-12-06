package fr.plopez.go4lunch.data.repositories

import fr.plopez.go4lunch.data.RestaurantDAO
import fr.plopez.go4lunch.data.model.restaurant.RestaurantQueryResponseItem
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantOpeningPeriodsCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantQueriesCrossReference
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.data.di.CoroutinesProvider
import fr.plopez.go4lunch.data.di.BuildConfigProvider
import fr.plopez.go4lunch.retrofit.RestaurantService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

@Singleton
class RestaurantsRepository @Inject constructor(
    private val restaurantService: RestaurantService,
    private val nearbyConstants: BuildConfigProvider,
    private val restaurantsCacheDAO: RestaurantDAO,
    private val coroutinesProvider: CoroutinesProvider
) {

    companion object {
        // this value is calculated for a tol of 50 m
        private const val MAX_DISPLACEMENT_TOL = "0.000449"
        private const val DETAILS_SEARCH_FIELD = "opening_hours,international_phone_number,website"
    }

    // Store the last requested time stamp for others view models get the correct list of restaurants
    private val lastRequestTimeStampMutableSharedFlow = MutableSharedFlow<Long>(replay = 1)
    val lastRequestTimeStampSharedFlow:Flow<Long> = lastRequestTimeStampMutableSharedFlow.asSharedFlow()

    suspend fun getRestaurantsAroundPosition(
        latitude: String,
        longitude: String
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
            if (restaurantsFromDatabase.first().restaurantList.isEmpty()) emit(ResponseStatus.NoRestaurants)
            else emit(ResponseStatus.Success(restaurantsFromDatabase.first().restaurantList))
            return@flow
        }

        // If no response of the database, check on google
        try {
            val location = "$latitude,$longitude"
            val response = restaurantService.getNearbyRestaurants(
                key = nearbyConstants.key,
                location = location
            )

            val responseBody = response.body()

            if (response.isSuccessful && responseBody != null) {

                // Getting details for restaurants
                val restaurantDetailsList: List<RestaurantDetails> =
                    getDetailsForRestaurants(responseBody.results)

                // Map query result to domain model
                val restaurantEntityList: List<RestaurantEntity> =
                    mapRestaurantQueryToEntity(responseBody.results, restaurantDetailsList)

                // Store restaurants in database in a separate coroutine
                withContext(coroutinesProvider.ioCoroutineDispatcher) {
                    storeInDatabase(
                        restaurantEntityList = restaurantEntityList,
                        restaurantDetailsList = restaurantDetailsList,
                        latitude = latitude,
                        longitude = longitude
                    )
                }

                // Emit the list of restaurant to the VM
                if (restaurantEntityList.isNotEmpty()) {
                    emit(ResponseStatus.Success(restaurantEntityList))
                } else {
                    emit(ResponseStatus.NoRestaurants)
                }

            } else {
                emit(ResponseStatus.NoResponse)
            }

        } catch (e: IOException) {
            emit(ResponseStatus.StatusError.IOException)

        } catch (e: HttpException) {
            emit(ResponseStatus.StatusError.HttpException)
        }
    }

    // Retrieve details for restaurants
    private suspend fun getDetailsForRestaurants(
        restaurantQueryResponseList: List<RestaurantQueryResponseItem>
    ): List<RestaurantDetails> =
        restaurantQueryResponseList.mapNotNull {

            try {
                val response = restaurantService.getDetailsForRestaurant(
                    key = nearbyConstants.key,
                    fields = DETAILS_SEARCH_FIELD,
                    placeId = it.placeID!!
                )

                val responseBody = response.body()

                if (response.isSuccessful && responseBody != null)
                    RestaurantDetails(
                        restaurantId = it.placeID,
                        website = responseBody.result?.website ?: "",
                        phoneNumber = responseBody.result?.phone_number ?: "",
                        periodList = responseBody.result?.opening_hours?.periods ?: emptyList(),
                        statusOk = true
                    )
                else null

            } catch (e: IOException) {
                null

            } catch (e: HttpException) {
                null
            }
        }

    // Store restaurants data from a nearby request into local SQLite database
    private suspend fun storeInDatabase(
        restaurantEntityList: List<RestaurantEntity>,
        restaurantDetailsList: List<RestaurantDetails>,
        latitude: String,
        longitude: String
    ) {

        // Create the current time stamp
        val timeStamp = System.currentTimeMillis()

        // Update the shared Flow to allow other view Models to request the right restaurants list
        lastRequestTimeStampMutableSharedFlow.emit(timeStamp)

        // Store Request in cache
        restaurantsCacheDAO.upsertQuery(
            RestaurantsQuery(
                timeStamp,
                latitude.toDouble(),
                longitude.toDouble()
            )
        )

        // Store all the attached restaurants
        restaurantEntityList.forEach { restaurant ->

            // Inserting restaurant queries cross ref
            restaurantsCacheDAO.upsertRestaurantQueriesCrossReference(
                RestaurantQueriesCrossReference(
                    timeStamp,
                    restaurant.restaurantId
                )
            )

            // Inserting restaurant entity
            restaurantsCacheDAO.upsertRestaurant(restaurant)

            // Getting restaurant details
            val restaurantDetails =
                restaurantDetailsList.find { it.restaurantId == restaurant.restaurantId }
                    ?: RestaurantDetails()

            // Store all periods of opening for a restaurant
            if (restaurantDetails.periodList.isNotEmpty()) {
                restaurantDetails.periodList.forEach { period ->

                    val day = period.open?.day
                    val openingHour = period.open?.time
                    val closingHour = period.close?.time

                    if (day != null &&
                        openingHour != null &&
                        closingHour != null
                    ) {
                        val periodId = "$day$openingHour$closingHour"

                        // Inserting Period
                        restaurantsCacheDAO.upsertRestaurantOpeningPeriod(
                            RestaurantOpeningPeriod(
                                periodId,
                                openingHour = LocalTime.of(
                                    openingHour.take(2).toInt(),
                                    openingHour.takeLast(2).toInt()
                                ).toString(),
                                closingHour = LocalTime.of(
                                    closingHour.take(2).toInt(),
                                    closingHour.takeLast(2).toInt()
                                ).toString(),
                                dayOfWeek = day
                            )
                        )

                        // Inserting restaurant - period cross ref
                        restaurantsCacheDAO.upsertRestaurantOpeningPeriodCrossReference(
                            RestaurantOpeningPeriodsCrossReference(
                                restaurant.restaurantId,
                                periodId
                            )
                        )
                    }
                }
            }
        }
    }

    // Map the query nearby object to domain object
    private fun mapRestaurantQueryToEntity(
        restaurantQueryResponseItemList: List<RestaurantQueryResponseItem>,
        restaurantDetailsList: List<RestaurantDetails>
    ): List<RestaurantEntity> =
        restaurantQueryResponseItemList.mapNotNull { restaurantQueryResponse ->
            if (restaurantQueryResponse.placeID != null
                && restaurantQueryResponse.name != null
                && restaurantQueryResponse.vicinity != null
                && restaurantQueryResponse.geometry?.location?.lat != null
                && restaurantQueryResponse.geometry.location.lng != null
            ) {
                val rate = restaurantQueryResponse.rating?.toFloat() ?: 0.0F
                val restaurantDetails =
                    restaurantDetailsList.find { it.restaurantId == restaurantQueryResponse.placeID }
                        ?: RestaurantDetails()

                RestaurantEntity(
                    restaurantId = restaurantQueryResponse.placeID,
                    name = restaurantQueryResponse.name,
                    address = restaurantQueryResponse.vicinity,
                    latitude = restaurantQueryResponse.geometry.location.lat,
                    longitude = restaurantQueryResponse.geometry.location.lng,
                    photoUrl = restaurantQueryResponse.photos?.firstOrNull()?.photoReference ?: "",
                    rate = round((rate * 3.0f) / 5.0f),
                    phoneNumber = restaurantDetails.phoneNumber,
                    website = restaurantDetails.website
                )
            } else {
                null
            }
        }

    // get list of restaurants with their opening periods
    suspend fun getRestaurantsWithOpeningPeriods(requestedTimeStamp: Long): List<RestaurantWithOpeningPeriods> {
        return if (requestedTimeStamp == 0L) {
            emptyList()
        } else {
            restaurantsCacheDAO.getCurrentRestaurants(requestedTimeStamp)
        }
    }

    suspend fun getPositionForTimestamp(timestamp: Long) =
        restaurantsCacheDAO.getPositionForTimestamp(timestamp)

    suspend fun getRestaurantFromId(id: String) =
        restaurantsCacheDAO.getRestaurantFromId(id)

    sealed class ResponseStatus {

        object NoResponse : ResponseStatus()

        object NoRestaurants : ResponseStatus()

        data class Success(val data: List<RestaurantEntity>) : ResponseStatus()

        sealed class StatusError : ResponseStatus() {
            object IOException : StatusError()
            object HttpException : StatusError()
        }
    }

    data class RestaurantDetails(
        val restaurantId: String = "",
        val website: String = "",
        val phoneNumber: String = "",
        val periodList: List<fr.plopez.go4lunch.data.model.restaurant.Period> = emptyList(),
        val statusOk: Boolean = false
    )
}