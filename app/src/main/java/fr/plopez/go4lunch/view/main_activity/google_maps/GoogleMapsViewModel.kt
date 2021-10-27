package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.utils.SingleLiveEvent
import fr.plopez.go4lunch.utils.exhaustive
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
    coroutinesProvider: CoroutinesProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Position with zoom and restaurants live data
    val googleMapViewStateLiveData: LiveData<GoogleMapViewState>

    private val googleMapViewActionSingleLiveEvent = SingleLiveEvent<GoogleMapViewAction>()
    val googleMapViewActionLiveData: LiveData<GoogleMapViewAction> = googleMapViewActionSingleLiveEvent

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
        // Initialization of the flow at init of the viewModel
        googleMapViewStateLiveData = onMapReadyMutableLiveData.switchMap {
            liveData(coroutinesProvider.ioCoroutineDispatcher) {

                locationRepository.fetchUpdates().collectLatest { positionWithZoom ->

                    // Send a retrofit request to fetch restaurants around received position
                    restaurantsRepository.getRestaurantsAroundPosition(
                        positionWithZoom.latitude.toString(),
                        positionWithZoom.longitude.toString(),
                        context.resources.getString(R.string.default_detection_radius_value)

                        // the flow will be collected only if the response is different from previous ones
                    ).distinctUntilChanged().collect {
                        when (it) {
                            is RestaurantsRepository.ResponseStatus.Success -> {
                                emit(mapDataToViewState(positionWithZoom, it.data))
                                withContext(coroutinesProvider.mainCoroutineDispatcher) {
                                    mapCamera(positionWithZoom)
                                }
                            }
                            is RestaurantsRepository.ResponseStatus.NoRestaurants -> {
                                emit(mapDataToViewState(positionWithZoom, emptyList()))
                                withContext(coroutinesProvider.mainCoroutineDispatcher) {
                                    mapCamera(positionWithZoom)
                                    mapEvent(Messages.NO_RESTAURANT)
                                }
                            }
                            is RestaurantsRepository.ResponseStatus.NoResponse ->
                                mapEvent(Messages.NO_RESPONSE)
                            is RestaurantsRepository.ResponseStatus.StatusError.HttpException ->
                                mapEvent(Messages.NETWORK_ERROR)
                            is RestaurantsRepository.ResponseStatus.StatusError.IOException ->
                                mapEvent(Messages.NO_INTERNET)
                        }.exhaustive
                    }
                }
            }
        }
    }

    // Just center the camera on the new position
    // The onFirstMapLoading is used to not animate camera when map is loaded. Only at first time.
    private fun mapCamera(positionWithZoom: LocationRepository.PositionWithZoom) {

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
        positionWithZoom: LocationRepository.PositionWithZoom,
        listRestaurants: List<RestaurantEntity>
    ) = GoogleMapViewState(
        latitude = positionWithZoom.latitude,
        longitude = positionWithZoom.longitude,
        zoom = positionWithZoom.zoom,
        restaurantList = listRestaurants.map {
            RestaurantViewState(
                latitude = it.latitude,
                longitude = it.longitude,
                name = it.name,
                id = it.restaurantId,
                iconDrawable = when (it.rate) {
                    1.0f -> R.drawable.red_pin_128px
                    2.0f -> R.drawable.orange_pin_128px
                    3.0f -> R.drawable.green_pin_128px
                    else -> R.drawable.grey_pin_128px
                }
            )
        }
    )

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

    sealed class GoogleMapViewAction {
        data class ResponseStatusMessage(@StringRes val messageResId: Int) : GoogleMapViewAction()
        data class MoveCamera(val latLng: LatLng, val zoom: Float) : GoogleMapViewAction()
        data class AnimateCamera(val latLng: LatLng, val zoom: Float) : GoogleMapViewAction()
    }

}