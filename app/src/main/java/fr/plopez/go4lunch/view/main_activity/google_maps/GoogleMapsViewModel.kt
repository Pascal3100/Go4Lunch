package fr.plopez.go4lunch.view.main_activity.google_maps

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
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

    companion object {
        private const val TAG = "Google Maps ViewModel"
    }

    // Position with zoom and restaurants flow
    private val googleMapViewStateMutableSharedFlow =
        MutableSharedFlow<GoogleMapViewState>(replay = 1)
    val googleMapViewStateLiveData = googleMapViewStateMutableSharedFlow.asLiveData(
        coroutinesProvider.ioCoroutineDispatcher
    )

    // Equivalent SingleLiveEvent with Flows
    private val googleMapViewActionChannel = Channel<GoogleMapViewAction>(Channel.BUFFERED)
    val googleMapViewActionFlow = googleMapViewActionChannel.receiveAsFlow()

    // Map state Flow
    private val onMapReadyMutableStateFlow = MutableStateFlow(false)

    // Booleans to manage camera
    private var onFirstMapLoading = true
    private var noRestaurantMessageAlreadyDisplayed = false

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
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {

            combine(
                onMapReadyMutableStateFlow, locationRepository.fetchUpdates()
            ) { isMapReady, positionWithZoom ->

                // Send an empty flow is map not ready
                if (!isMapReady) return@combine null

                // Send a retrofit request to fetch restaurants around received position
                restaurantsRepository.getRestaurantsAroundPosition(
                    positionWithZoom.latitude.toString(),
                    positionWithZoom.longitude.toString(),
                    context.resources.getString(R.string.default_detection_radius_value)
                    // the flow will be collected only if the response is different from previous ones
                ).distinctUntilChanged().collect {
                    when (it) {
                        is RestaurantsRepository.ResponseStatus.Success -> {
                            mapData(positionWithZoom, it.data)
                            mapCamera(positionWithZoom)
                        }
                        is RestaurantsRepository.ResponseStatus.NoRestaurants -> {
                            mapData(positionWithZoom, emptyList())
                            mapCamera(positionWithZoom)
                            mapEvent(Messages.NO_RESTAURANT)
                        }
                        is RestaurantsRepository.ResponseStatus.NoResponse ->
                            mapEvent(Messages.NO_RESPONSE)
                        is RestaurantsRepository.ResponseStatus.StatusError.HttpException ->
                            mapEvent(Messages.NETWORK_ERROR)
                        is RestaurantsRepository.ResponseStatus.StatusError.IOException ->
                            mapEvent(Messages.NO_INTERNET)
                    }
                }
            }.collect()
        }
    }

    // Just center the camera on the new position
    // The onFirstMapLoading is used to not animate camera when map is loaded. Only at first time.
    private suspend fun mapCamera(positionWithZoom: LocationRepository.PositionWithZoom) {

        val data = CameraUpdateFactory.newLatLngZoom(
            LatLng(
                positionWithZoom.latitude,
                positionWithZoom.longitude
            ),
            positionWithZoom.zoom
        )

        if (onFirstMapLoading) {
            onFirstMapLoading = false
            googleMapViewActionChannel.send(GoogleMapViewAction.MoveCamera(data))
        } else {
            googleMapViewActionChannel.send(GoogleMapViewAction.AnimateCamera(data))
        }
    }

    // Track Map status
    fun onMapReady() {
        onMapReadyMutableStateFlow.value = true
    }

    // Track zoom changes
    fun onZoomChanged(zoom: Float) {
        locationRepository.currentZoom = zoom
    }

    // Manage event message sending in flows
    private suspend fun mapEvent(
        message: Messages
    ) {
        if (!noRestaurantMessageAlreadyDisplayed) {
            googleMapViewActionChannel.send(
                GoogleMapViewAction.ResponseStatusMessage(message.messageResId)
            )
        }
        noRestaurantMessageAlreadyDisplayed = message == Messages.NO_RESTAURANT
    }

    // Manage data sending in flows
    private fun mapData(
        positionWithZoom: LocationRepository.PositionWithZoom,
        listRestaurants: List<RestaurantEntity>,
    ) {
        // Send cur position and list of around restaurants
        googleMapViewStateMutableSharedFlow.tryEmit(
            mapToViewState(positionWithZoom, listRestaurants)
        )
    }

    // Mapper for the ui view state
    private fun mapToViewState(
        positionWithZoom: LocationRepository.PositionWithZoom,
        listRestaurants: List<RestaurantEntity>
    ): GoogleMapViewState {
        val restaurantViewStateList = listRestaurants.map {
            @DrawableRes
            val iconDrawable = when (it.rate) {
                1.0f -> R.drawable.red_pin_128px
                2.0f -> R.drawable.orange_pin_128px
                3.0f -> R.drawable.green_pin_128px
                else -> R.drawable.grey_pin_128px
            }

            RestaurantViewState(
                latitude = it.latitude,
                longitude = it.longitude,
                name = it.name,
                id = it.restaurantId,
                iconDrawable = iconDrawable
            )
        }

        return GoogleMapViewState(
            positionWithZoom.latitude,
            positionWithZoom.longitude,
            positionWithZoom.zoom,
            Collections.unmodifiableList(restaurantViewStateList)
        )
    }

    fun onFirstMapLoading() {
        onFirstMapLoading = true
    }

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
        data class MoveCamera(val data: CameraUpdate) : GoogleMapViewAction()
        data class AnimateCamera(val data: CameraUpdate) : GoogleMapViewAction()
    }

}