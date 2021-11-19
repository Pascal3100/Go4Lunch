package fr.plopez.go4lunch.view.restaurant_details

import android.annotation.SuppressLint
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
import fr.plopez.go4lunch.utils.FirebaseAuthUtils
import fr.plopez.go4lunch.view.model.RestaurantDetailsViewState
import fr.plopez.go4lunch.view.model.WorkmateViewState
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewModel.RestaurantDetailsViewAction.FirestoreFails
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewModel.RestaurantDetailsViewAction.FirestoreWorks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ExperimentalCoroutinesApi
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class RestaurantDetailsViewModel @Inject constructor(
    private val restaurantsRepository: RestaurantsRepository,
    private val firestoreRepository: FirestoreRepository,
    private val firebaseAuthUtils: FirebaseAuthUtils,
    private val coroutinesProvider: CoroutinesProvider,
    private val nearbyConstants: NearbyConstants,
    state: SavedStateHandle,
    @ApplicationContext private val context: Context

) : ViewModel() {

    companion object {
        private const val MAX_WIDTH = "1080"
    }

    // This because we use SavedStateHandle
    // It allows to transform directly the id passed to the activity by the bundle
    // to the restaurant entity
    private val restaurantEntityLiveData = state.getLiveData<String>("PLACE_ID").switchMap {
        liveData(coroutinesProvider.ioCoroutineDispatcher) {
            emit(restaurantsRepository.getRestaurantFromId(it))
        }
    }

    val restaurantDetailsViewLiveData: LiveData<RestaurantDetailsViewState>

    private val firestoreStateMutableLiveData =
        MutableLiveData<RestaurantDetailsViewAction>()
    val firestoreStateLiveData =
        firestoreStateMutableLiveData as LiveData<RestaurantDetailsViewAction>

    private var likeState = false
    private var selectedState = false

    private val user = firebaseAuthUtils.getUser()

    init {
        restaurantDetailsViewLiveData =
            restaurantEntityLiveData.switchMap { restaurantEntity ->
                liveData(coroutinesProvider.ioCoroutineDispatcher) {
                    combine(
                        firestoreRepository.getWorkmatesWithSelectedRestaurants(),
                        firestoreRepository.getLikedRestaurants()
                    ) { workmatesWithSelectedRestaurantsList, likedRestaurantsList ->
                        mapToViewState(
                            restaurantEntity = restaurantEntity,
                            isLiked = restaurantEntity.restaurantId in likedRestaurantsList,
                            isSelected = isRestaurantSelected(
                                restaurantEntity.restaurantId,
                                workmatesWithSelectedRestaurantsList
                            ),
                            interestedWorkmatesList = getInterestedWorkmatesList(
                                restaurantEntity.restaurantId,
                                workmatesWithSelectedRestaurantsList
                            )
                        )
                    }.collect {
                        emit(it)
                        likeState = it.isFavorite
                        selectedState = it.isSelected
                    }
                }
            }
    }

    private fun getInterestedWorkmatesList(
        placeId: String,
        workmatesWithSelectedRestaurantsList: List<WorkmateWithSelectedRestaurant>
    ) = workmatesWithSelectedRestaurantsList.filter {
        it.selectedRestaurantId == placeId
    }.map {
        WorkmateViewState(
            photoUrl = it.workmatePhotoUrl,
            text = context.resources.getString(R.string.joined_workmate_message, it.workmateName),
            style = R.style.workmateItemNormalBlackBoldTextAppearance
        )
    }

    private fun isRestaurantSelected(
        placeId: String,
        workmatesWithSelectedRestaurantsList: List<WorkmateWithSelectedRestaurant>
    ) = workmatesWithSelectedRestaurantsList.any {
        it.workmateEmail == user.email && it.selectedRestaurantId == placeId
    }

    private fun mapToViewState(
        restaurantEntity: RestaurantEntity,
        isLiked: Boolean,
        isSelected: Boolean,
        interestedWorkmatesList: List<WorkmateViewState>
    ): RestaurantDetailsViewState =
        RestaurantDetailsViewState(
            photoUrl = mapRestaurantPhotoUrl(restaurantEntity.photoUrl),
            name = restaurantEntity.name,
            address = restaurantEntity.address,
            rate = restaurantEntity.rate,
            phoneNumber = restaurantEntity.phoneNumber,
            website = restaurantEntity.website,
            isFavorite = isLiked,
            isSelected = isSelected,
            interestedWorkmatesList = interestedWorkmatesList
        )

    // when the current restaurant is liked by the current user
    fun onLike() {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            restaurantEntityLiveData.asFlow().collect {
                val success =
                    firestoreRepository.addOrSuppressLikedRestaurant(
                        placeId = it.restaurantId,
                        likeState = likeState
                    )

                // back to initial state if firestore fails
                withContext(coroutinesProvider.mainCoroutineDispatcher) {
                    if (!success) {
                        firestoreStateMutableLiveData.value = FirestoreFails
                    } else {
                        firestoreStateMutableLiveData.value = FirestoreWorks
                    }
                }
            }
        }
    }

    fun onSelectRestaurant() {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher) {
            restaurantEntityLiveData.asFlow().collect {
                val success = firestoreRepository.setOrUnsetSelectedRestaurant(
                    placeId = it.restaurantId,
                    selectedState = selectedState
                )

                // back to initial state if firestore fails
                withContext(coroutinesProvider.mainCoroutineDispatcher) {
                    if (!success) {
                        firestoreStateMutableLiveData.value = FirestoreFails
                    } else {
                        firestoreStateMutableLiveData.value = FirestoreWorks
                    }
                }
            }
        }
    }

    // Retrieve photo Url per restaurant
    private fun mapRestaurantPhotoUrl(photoReference: String?): String {
        return if (photoReference != null && photoReference != "") {
            context.resources.getString(
                R.string.place_photo_api_url,
                MAX_WIDTH,
                photoReference,
                nearbyConstants.key
            )
        } else {
            ""
        }
    }

    sealed class RestaurantDetailsViewAction {
        object FirestoreFails : RestaurantDetailsViewAction()
        object FirestoreWorks : RestaurantDetailsViewAction()
    }

}