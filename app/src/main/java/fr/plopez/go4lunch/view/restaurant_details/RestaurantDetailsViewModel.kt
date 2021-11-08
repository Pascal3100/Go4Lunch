package fr.plopez.go4lunch.view.restaurant_details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.di.NearbyConstants
import fr.plopez.go4lunch.view.model.RestaurantDetailsViewState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class RestaurantDetailsViewModel @Inject constructor(
    private val restaurantsRepository: RestaurantsRepository,
    coroutinesProvider: CoroutinesProvider,
    private val nearbyConstants: NearbyConstants,
    @ApplicationContext private val context: Context

) : ViewModel() {

    companion object{
        private const val MAX_WIDTH = 1080
    }

    private val placeIdMutableStateFlow = MutableStateFlow<String?>(null)

    private val restaurantDetailsViewStateMutableSharedFlow =
        MutableSharedFlow<RestaurantDetailsViewState>(replay = 1)
    val restaurantDetailsViewLiveData = restaurantDetailsViewStateMutableSharedFlow.asLiveData(
        coroutinesProvider.ioCoroutineDispatcher
    )

    private val likeStateMutableStateFlow = MutableStateFlow(false)
    val likeStateLiveData = likeStateMutableStateFlow.asLiveData(
        coroutinesProvider.ioCoroutineDispatcher
    )

    private val restaurantSelectedStateMutableStateFlow = MutableStateFlow(false)
    val restaurantSelectedStateLiveData = restaurantSelectedStateMutableStateFlow.asLiveData(
        coroutinesProvider.ioCoroutineDispatcher
    )

    init {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            placeIdMutableStateFlow.filterNotNull().map { placeId ->
                restaurantsRepository.getRestaurantFromId(placeId)
            }.collect { restaurantEntity ->
                restaurantDetailsViewStateMutableSharedFlow.emit(mapToViewState(restaurantEntity))
            }
        }
    }

    private fun mapToViewState(restaurantEntity: RestaurantEntity): RestaurantDetailsViewState =
        RestaurantDetailsViewState(
            photoUrl = mapRestaurantPhotoUrl(restaurantEntity.photoUrl),
            name = restaurantEntity.name,
            address = restaurantEntity.address,
            rate = restaurantEntity.rate,
            phoneNumber = restaurantEntity.phoneNumber,
            website = restaurantEntity.website,
            // TODO ces deux valeurs doivent venir d'un mappeur et du repo
            isFavorite = false,
            isSelected = false
        )

    fun onPlaceIdRequest(id: String?) {
        placeIdMutableStateFlow.value = id
    }

    // Retrieve photo Url per restaurant
    private fun mapRestaurantPhotoUrl(photoReference: String?): String {
        return if (photoReference != null && photoReference != "") {
            context.resources.getString(R.string.place_photo_api_url) +
                    "maxwidth=${MAX_WIDTH}&" +
                    "photoreference=$photoReference&" +
                    "key=${nearbyConstants.key}"
        } else {
            ""
        }
    }

    fun onLike() {
        likeStateMutableStateFlow.value = !likeStateMutableStateFlow.value
    }

    fun onSelectRestaurant() {
        restaurantSelectedStateMutableStateFlow.value = !restaurantSelectedStateMutableStateFlow.value
    }

}