package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.LocationRepository.*
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository.ResponseStatus
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.utils.SingleLiveEvent
import fr.plopez.go4lunch.utils.exhaustive
import fr.plopez.go4lunch.view.main_activity.SearchUseCase
import fr.plopez.go4lunch.view.main_activity.SearchUseCase.SearchResultStatus.SearchResult
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ExperimentalCoroutinesApi
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class GoogleMapsViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val restaurantsRepository: RestaurantsRepository,
    private val firestoreRepository: FirestoreRepository,
    private val searchUseCase: SearchUseCase,
    coroutinesProvider: CoroutinesProvider,
) : ViewModel() {

    // Position with zoom and restaurants live data
    val googleMapViewStateLiveData: LiveData<GoogleMapViewState>

    private val googleMapViewActionSingleLiveEvent = SingleLiveEvent<GoogleMapViewAction>()
    val googleMapViewActionLiveData: LiveData<GoogleMapViewAction> =
        googleMapViewActionSingleLiveEvent

    // Map state Flow
    private val onMapReadyMutableLiveData = MutableLiveData<Unit>()

    // Booleans to manage camera
    private var isSmoothCameraUpdateDone = false
    private var isRestaurantMessageAlreadyDisplayed = false

    enum class Messages(
        @get:StringRes
        val messageResId: Int
    ) {
        NO_RESTAURANT(messageResId = R.string.no_restaurants_message),
        NO_RESPONSE(messageResId = R.string.no_response_message),
        NO_INTERNET(messageResId = R.string.network_error_message),
        NETWORK_ERROR(messageResId = R.string.no_internet_message)
    }

    init {
        googleMapViewStateLiveData = onMapReadyMutableLiveData.switchMap {

            liveData(coroutinesProvider.ioCoroutineDispatcher) {
                locationRepository.fetchUpdates()
                    .transformLatest { positionWithZoom ->
                        // Send a retrofit request to fetch restaurants around received position
                        // the flow will be collected only if the response is different from previous ones
                        restaurantsRepository.getRestaurantsAroundPosition(
                            latitude = positionWithZoom.latitude.toString(),
                            longitude = positionWithZoom.longitude.toString()
                        ).distinctUntilChanged()
                            .combine(searchUseCase.getSearchResult()) { responseStatus, searchResultStatus ->
                                Log.d("TAG", "#### searchResultStatus: $searchResultStatus")

                                if (responseStatus is ResponseStatus.Success && searchResultStatus is SearchResult) {
                                    ResponseStatus.Success(
                                        data = responseStatus.data.filter { it.restaurantId in searchResultStatus.data }
                                    )
                                } else {
                                    responseStatus
                                }
                            }.collect {
                            emit(
                                PositionWithZoomAndResponseStatus(
                                    positionWithZoom = positionWithZoom,
                                    responseStatus = it
                                )
                            )
                        }
                    }
                    .combine(firestoreRepository.getWorkmatesWithSelectedRestaurants()) { positionWithZoomAndResponseStatus, workmatesWithSelectedRestaurants ->
                        Pair(positionWithZoomAndResponseStatus, workmatesWithSelectedRestaurants)
                    }.collect {
                        val positionWithZoom = it.first.positionWithZoom
                        val responseStatus = it.first.responseStatus
                        val workmatesWithSelectedRestaurants = it.second

                        when (responseStatus) {
                            is ResponseStatus.Success -> {
                                emit(
                                    mapDataToViewState(
                                        positionWithZoom,
                                        responseStatus.data,
                                        workmatesWithSelectedRestaurants
                                    )
                                )
                                withContext(coroutinesProvider.mainCoroutineDispatcher) {
                                    mapCamera(positionWithZoom)
                                }
                            }
                            is ResponseStatus.NoRestaurants -> {
                                emit(mapDataToViewState(positionWithZoom))
                                withContext(coroutinesProvider.mainCoroutineDispatcher) {
                                    mapCamera(positionWithZoom)
                                    mapEvent(Messages.NO_RESTAURANT)
                                }
                            }
                            is ResponseStatus.NoResponse ->
                                mapEvent(Messages.NO_RESPONSE)
                            is ResponseStatus.StatusError.HttpException ->
                                mapEvent(Messages.NETWORK_ERROR)
                            is ResponseStatus.StatusError.IOException ->
                                mapEvent(Messages.NO_INTERNET)
                        }.exhaustive
                    }
            }
        }
    }

    // Just center the camera on the new position
    // The onFirstMapLoading is used to not animate camera when map is loaded. Only at first time.
    private fun mapCamera(positionWithZoom: PositionWithZoom) {

        val data = Pair(
            LatLng(
                positionWithZoom.latitude,
                positionWithZoom.longitude
            ),
            positionWithZoom.zoom
        )

        if (!isSmoothCameraUpdateDone) {
            isSmoothCameraUpdateDone = true
            googleMapViewActionSingleLiveEvent.value =
                GoogleMapViewAction.MoveCamera(
                    latLng = data.first,
                    zoom = data.second
                )
        } else {
            googleMapViewActionSingleLiveEvent.value =
                GoogleMapViewAction.AnimateCamera(
                    latLng = data.first,
                    zoom = data.second
                )
        }
    }

    // Track Map status via a simple ping
    fun onMapReady() {
        onMapReadyMutableLiveData.value = Unit
    }

    // Track zoom changes
    fun onZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    // Manage event message sending in flows
    private fun mapEvent(
        message: Messages
    ) {
        if (!isRestaurantMessageAlreadyDisplayed) {
            googleMapViewActionSingleLiveEvent.value =
                GoogleMapViewAction.ResponseStatusMessage(message.messageResId)
        }
        isRestaurantMessageAlreadyDisplayed = message == Messages.NO_RESTAURANT
    }

    // Mapper for the ui view state
    private fun mapDataToViewState(
        positionWithZoom: PositionWithZoom,
        listRestaurants: List<RestaurantEntity> = emptyList(),
        workmatesWithSelectedRestaurants: List<WorkmateWithSelectedRestaurant> = emptyList()
    ) = GoogleMapViewState(
        latitude = positionWithZoom.latitude,
        longitude = positionWithZoom.longitude,
        zoom = positionWithZoom.zoom,
        restaurantList = listRestaurants.map { restaurantEntity ->
            val isSelectedRestaurant = workmatesWithSelectedRestaurants.any {
                restaurantEntity.restaurantId in it.selectedRestaurantId
            }
            RestaurantViewState(
                latitude = restaurantEntity.latitude,
                longitude = restaurantEntity.longitude,
                name = restaurantEntity.name,
                id = restaurantEntity.restaurantId,
                iconDrawable = if (isSelectedRestaurant) {
                    when (restaurantEntity.rate) {
                        1.0f -> R.drawable.red_pin_star_128px
                        2.0f -> R.drawable.orange_pin_star_128px
                        3.0f -> R.drawable.green_pin_star_128px
                        else -> R.drawable.grey_pin_star_128px
                    }
                } else {
                    when (restaurantEntity.rate) {
                        1.0f -> R.drawable.red_pin_128px
                        2.0f -> R.drawable.orange_pin_128px
                        3.0f -> R.drawable.green_pin_128px
                        else -> R.drawable.grey_pin_128px
                    }
                }
            )
        })

    // Data Class to emit to the UI
    data class GoogleMapViewState(
        val latitude: Double,
        val longitude: Double,
        val zoom: Float,
        val restaurantList: List<RestaurantViewState>
    )

    // Data Class to emit to the UI
    data class RestaurantViewState(
        val latitude: Double,
        val longitude: Double,
        val name: String,
        val id: String,
        @DrawableRes
        val iconDrawable: Int
    )

    data class PositionWithZoomAndResponseStatus(
        val positionWithZoom: PositionWithZoom,
        val responseStatus: ResponseStatus
    )

    sealed class GoogleMapViewAction {
        data class ResponseStatusMessage(@StringRes val messageResId: Int) :
            GoogleMapViewAction()

        data class MoveCamera(val latLng: LatLng, val zoom: Float) : GoogleMapViewAction()
        data class AnimateCamera(val latLng: LatLng, val zoom: Float) :
            GoogleMapViewAction()
    }
}