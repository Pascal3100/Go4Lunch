package fr.plopez.go4lunch.view.restaurant_details

import android.content.Context
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.di.NearbyConstants
import fr.plopez.go4lunch.view.model.RestaurantDetailsViewState
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewModel.RestaurantDetailsViewAction.FirestoreFails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class RestaurantDetailsViewModel @Inject constructor(
    private val restaurantsRepository: RestaurantsRepository,
    private val firestoreRepository: FirestoreRepository,
    private val firebaseAuth: FirebaseAuth,
    private val coroutinesProvider: CoroutinesProvider,
    private val nearbyConstants: NearbyConstants,
    @ApplicationContext private val context: Context

) : ViewModel() {

    companion object {
        private const val MAX_WIDTH = 1080
    }

    private val placeIdMutableStateFlow = MutableStateFlow<String?>(null)

    private val restaurantDetailsViewStateMutableLiveData =
        MutableLiveData<RestaurantDetailsViewState>()
    val restaurantDetailsViewLiveData =
        restaurantDetailsViewStateMutableLiveData as LiveData<RestaurantDetailsViewState>

    private val firestoreStateMutableLiveData =
        MutableLiveData<RestaurantDetailsViewAction>()
    val firestoreStateLiveData = firestoreStateMutableLiveData as LiveData<RestaurantDetailsViewAction>

    private var likeState = false
    private var selectedRestaurantState = false

    init {

        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            combine(placeIdMutableStateFlow, firestoreRepository.getLikedRestaurants()){ placeId, likedRestaurantsList ->
                if (placeId == null) return@combine null
                mapToViewState(
                    restaurantEntity = restaurantsRepository.getRestaurantFromId(placeId),
                    isLiked = placeId in likedRestaurantsList
                )
            }.collect {
                withContext(coroutinesProvider.mainCoroutineDispatcher) {
                    restaurantDetailsViewStateMutableLiveData.value = it
                    likeState = it!!.isFavorite
                }
            }
        }
    }

    private fun mapToViewState(
        restaurantEntity: RestaurantEntity,
        isLiked : Boolean
    ): RestaurantDetailsViewState =
        RestaurantDetailsViewState(
            photoUrl = mapRestaurantPhotoUrl(restaurantEntity.photoUrl),
            name = restaurantEntity.name,
            address = restaurantEntity.address,
            rate = restaurantEntity.rate,
            phoneNumber = restaurantEntity.phoneNumber,
            website = restaurantEntity.website,
            // TODO ces deux valeurs doivent venir d'un mappeur et du repo
            isFavorite = isLiked,
            isSelected = false
        )

    fun onPlaceIdRequest(id: String) {
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

        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher){
            val placeId = placeIdMutableStateFlow.value

            val success = firestoreRepository.addOrSuppressLikedRestaurant(placeId, likeState)

            // back to initial state if firestore fails
            if (!success) {
                withContext(coroutinesProvider.mainCoroutineDispatcher){
                    firestoreStateMutableLiveData.value = FirestoreFails
                }
            }
        }
    }

    fun onSelectRestaurant() {
    }

    sealed class RestaurantDetailsViewAction{
        object FirestoreFails : RestaurantDetailsViewAction()

    }

}