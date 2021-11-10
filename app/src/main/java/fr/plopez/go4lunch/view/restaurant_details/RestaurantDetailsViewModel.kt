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
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
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
    val firestoreStateLiveData =
        firestoreStateMutableLiveData as LiveData<RestaurantDetailsViewAction>

    private var likeState = false
    private var selectedState = false

    init {

        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            combine(
                placeIdMutableStateFlow,
                firestoreRepository.getLikedRestaurants(),
                firestoreRepository.getWorkmatesWithSelectedRestaurants()
            ) { placeId, likedRestaurantsList, workmatesWithSelectedRestaurantsList ->
                if (placeId == null) return@combine null
                mapToViewState(
                    restaurantEntity = restaurantsRepository.getRestaurantFromId(placeId),
                    isLiked = placeId in likedRestaurantsList,
                    isSelected = isRestaurantSelected(placeId, workmatesWithSelectedRestaurantsList)
                )
            }.collect {
                withContext(coroutinesProvider.mainCoroutineDispatcher) {
                    if (it != null) {
                        restaurantDetailsViewStateMutableLiveData.value = it
                        likeState = it.isFavorite
                        selectedState = it.isSelected
                    }
                }
            }
        }
    }

    private fun isRestaurantSelected(
        placeId: String,
        workmatesWithSelectedRestaurantsList: List<WorkmateWithSelectedRestaurant>
    ): Boolean {
        return workmatesWithSelectedRestaurantsList.any {
            it.workmateEmail == firebaseAuth.currentUser?.email && it.selectedRestaurantId == placeId
        }
    }

    private fun mapToViewState(
        restaurantEntity: RestaurantEntity,
        isLiked: Boolean,
        isSelected: Boolean
    ): RestaurantDetailsViewState =
        RestaurantDetailsViewState(
            photoUrl = mapRestaurantPhotoUrl(restaurantEntity.photoUrl),
            name = restaurantEntity.name,
            address = restaurantEntity.address,
            rate = restaurantEntity.rate,
            phoneNumber = restaurantEntity.phoneNumber,
            website = restaurantEntity.website,
            isFavorite = isLiked,
            isSelected = isSelected
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

    // when the current restaurant is liked by the current user
    fun onLike() {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            placeIdMutableStateFlow.collect { placeId ->
                if (placeId != null) {
                    val success =
                        firestoreRepository.addOrSuppressLikedRestaurant(
                            placeId = placeId,
                            likeState = likeState)

                    // back to initial state if firestore fails
                    if (!success) {
                        withContext(coroutinesProvider.mainCoroutineDispatcher) {
                            firestoreStateMutableLiveData.value = FirestoreFails
                        }
                    }
                }
            }
        }
    }

    fun onSelectRestaurant() {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            placeIdMutableStateFlow.collect { placeId ->
                if (placeId != null) {
                    val success = firestoreRepository.setOrUnsetSelectedRestaurant(
                        placeId = placeId,
                        selectedState = selectedState
                    )

                    // back to initial state if firestore fails
                    if (!success) {
                        withContext(coroutinesProvider.mainCoroutineDispatcher) {
                            firestoreStateMutableLiveData.value = FirestoreFails
                        }
                    }
                }
            }
        }
    }

    sealed class RestaurantDetailsViewAction {
        object FirestoreFails : RestaurantDetailsViewAction()

    }

}