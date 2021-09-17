package fr.plopez.go4lunch.data.repositories

import fr.plopez.go4lunch.data.model.restaurant.Restaurant
import fr.plopez.go4lunch.di.NearbyParameters
import fr.plopez.go4lunch.interfaces.RestaurantNearbyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class RestaurantsRepository @Inject constructor(
    private val restaurantNearbyService: RestaurantNearbyService
) {

    private val responseStatusMutableStateFlow = MutableStateFlow<ResponseStatus>(ResponseStatus.Empty)
    val responseStatusStateFlow = responseStatusMutableStateFlow.asStateFlow()
    @Inject lateinit var nearbyParameters: NearbyParameters

    suspend fun getRestaurantsAroundPosition(
        latitude: String,
        longitude: String,
        radius: String,
    ): List<Restaurant> {
        try {
            val location = "$latitude,$longitude"
            val response = restaurantNearbyService.getNearbyRestaurants(
                nearbyParameters.key,
                nearbyParameters.type,
                location,
                radius
            )

            if (response.isSuccessful && response.body() != null){
                responseStatusMutableStateFlow.value = ResponseStatus.ResponseOk
                return response.body()!!
            } else {
                responseStatusMutableStateFlow.value = ResponseStatus.NoResponse
            }

        } catch (e: IOException) {
            responseStatusMutableStateFlow.value = ResponseStatus.StatusError.IOException

        } catch (e: HttpException) {
            responseStatusMutableStateFlow.value = ResponseStatus.StatusError.HttpException
        }

        // Return an empty list for all non ok states
        return emptyList()
    }

    sealed class ResponseStatus {

        object Empty:ResponseStatus()

        object NoResponse:ResponseStatus()

        object ResponseOk:ResponseStatus()

        sealed class StatusError:ResponseStatus(){
            object IOException:StatusError()
            object HttpException:StatusError()
        }

    }
}