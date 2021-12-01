package fr.plopez.go4lunch.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import fr.plopez.go4lunch.data.Workmate
import fr.plopez.go4lunch.utils.DateTimeUtils
import fr.plopez.go4lunch.utils.FirebaseAuthUtils
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ExperimentalCoroutinesApi
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val restaurantsRepository: RestaurantsRepository,
    private val firebaseAuthUtils: FirebaseAuthUtils,
    private val dateTimeUtils: DateTimeUtils
) {
    companion object {
        private const val WORKMATES_COLLECTION = "workmates"
        private const val LIKED_RESTAURANTS_COLLECTION = "liked_restaurants"
        private const val DATES_COLLECTION = "dates"
        private const val INTERESTED_WORKMATES_COLLECTION = "interested_workmates"
        private const val ID_FIELD = "id"
        private const val SETTINGS = "settings"
        private const val NOTIFICATIONS_SETTINGS = "notifications"
    }

    // current user info
    private val user = firebaseAuthUtils.getUser()

    // add user in firestore database or update it if exists
    suspend fun addOrUpdateUserOnLogin() =
        suspendCancellableCoroutine<Boolean> { continuation ->
            firestore.collection(WORKMATES_COLLECTION)
                .document(user.email)
                .set(user, SetOptions.merge())
                .addOnSuccessListener {
                    continuation.resume(value = true, onCancellation = null)
                }
                .addOnFailureListener {
                    continuation.resume(value = false, onCancellation = null)
                }
        }

    // Listener for workmates collection updates
    suspend fun getWorkmatesUpdates(): Flow<List<Workmate>> = callbackFlow {
        val workmatesCollection = firestore.collection(WORKMATES_COLLECTION)

        val workmatesListener = workmatesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val updatedWorkmatesList = snapshot.mapNotNull {
                    firebaseAuthUtils.getWorkmate(
                        displayName = it.getString("name"),
                        email = it.getString("email"),
                        photoUrl = it.getString("photoUrl")
                    )
                }
                trySend(updatedWorkmatesList)
            }
        }
        awaitClose { workmatesListener.remove() }
    }

    // Returns the ids of all liked restaurants by the current user
    fun getLikedRestaurants(): Flow<List<String>> = callbackFlow {
        val likedRestaurantsCollection = firestore
            .collection(WORKMATES_COLLECTION)
            .document(user.email)
            .collection(LIKED_RESTAURANTS_COLLECTION)

        val likedRestaurantsListener =
            likedRestaurantsCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val likedRestaurantsList = snapshot.mapNotNull {
                        it.getString(ID_FIELD)
                    }
                    trySend(likedRestaurantsList)
                }
            }

        awaitClose { likedRestaurantsListener.remove() }
    }

    // Manage the liked status of the restaurant
    suspend fun addOrSuppressLikedRestaurant(placeId: String, likeState: Boolean) =
        suspendCancellableCoroutine<Boolean> { continuation ->
            if (!likeState) {
                firestore.collection(WORKMATES_COLLECTION)
                    .document(user.email)
                    .collection(LIKED_RESTAURANTS_COLLECTION)
                    .document(placeId)
                    .set(
                        hashMapOf(
                            ID_FIELD to placeId
                        )
                    )
                    .addOnSuccessListener {
                        continuation.resume(value = true, onCancellation = null)
                    }
                    .addOnFailureListener {
                        continuation.resume(value = false, onCancellation = null)
                    }
            } else {
                firestore.collection(WORKMATES_COLLECTION)
                    .document(user.email)
                    .collection(LIKED_RESTAURANTS_COLLECTION)
                    .document(placeId)
                    .delete()
                    .addOnSuccessListener {
                        continuation.resume(value = true, onCancellation = null)
                    }
                    .addOnFailureListener {
                        continuation.resume(value = false, onCancellation = null)
                    }
            }
        }

    // Manage the selected status of the restaurant
    suspend fun setOrUnsetSelectedRestaurant(
        placeId: String,
        selectedState: Boolean
    ): Boolean {
        val restaurantData = restaurantsRepository.getRestaurantFromId(placeId)

        return suspendCancellableCoroutine { continuation ->
            if (!selectedState) {
                firestore
                    .collection(DATES_COLLECTION)
                    .document(dateTimeUtils.getCurrentDate())
                    .collection(INTERESTED_WORKMATES_COLLECTION)
                    .document(user.email)
                    .set(
                        WorkmateWithSelectedRestaurant(
                            workmateName = user.name,
                            workmateEmail = user.email,
                            workmatePhotoUrl = user.photoUrl,
                            selectedRestaurantId = restaurantData.restaurantId,
                            selectedRestaurantName = restaurantData.name
                        ), SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        continuation.resume(value = true, onCancellation = null)
                    }
                    .addOnFailureListener {
                        continuation.resume(value = false, onCancellation = null)
                    }
            } else {
                firestore
                    .collection(DATES_COLLECTION)
                    .document(dateTimeUtils.getCurrentDate())
                    .collection(INTERESTED_WORKMATES_COLLECTION)
                    .document(user.email)
                    .delete()
                    .addOnSuccessListener {
                        continuation.resume(value = true, onCancellation = null)
                    }
                    .addOnFailureListener {
                        continuation.resume(value = false, onCancellation = null)
                    }
            }
        }
    }

    // get all workmates which have selected a restaurant
    suspend fun getWorkmatesWithSelectedRestaurants(): Flow<List<WorkmateWithSelectedRestaurant>> =
        callbackFlow {
            val interestedWorkmatesCollection = firestore
                .collection(DATES_COLLECTION)
                .document(dateTimeUtils.getCurrentDate())
                .collection(INTERESTED_WORKMATES_COLLECTION)

            val selectedRestaurantsListener =
                interestedWorkmatesCollection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {

                        val interestedWorkmatesWithSelectedRestaurantList = snapshot.mapNotNull {
                            val workmate = firebaseAuthUtils.getUser(
                                displayName = it.getString("workmateName"),
                                email = it.getString("workmateEmail"),
                                photoUrl = it.getString("workmatePhotoUrl")
                            )
                            WorkmateWithSelectedRestaurant(
                                workmateName = workmate.name,
                                workmateEmail = workmate.email,
                                workmatePhotoUrl = workmate.photoUrl,
                                selectedRestaurantName = it.getString("selectedRestaurantName")!!,
                                selectedRestaurantId = it.getString("selectedRestaurantId")!!
                            )
                        }
                        trySend(interestedWorkmatesWithSelectedRestaurantList)
                    }
                }

            awaitClose { selectedRestaurantsListener.remove() }
        }

    // update notification setting for current user
    suspend fun setNotificationsSetting(notificationsAccepted: Boolean) =
        suspendCancellableCoroutine<Boolean> { continuation ->
            firestore
                .collection(WORKMATES_COLLECTION)
                .document(user.email)
                .collection(SETTINGS)
                .document(NOTIFICATIONS_SETTINGS)
                .set(
                    hashMapOf(
                        "accept" to notificationsAccepted
                    )
                )
                .addOnSuccessListener {
                    continuation.resume(value = true, onCancellation = null)
                }
                .addOnFailureListener {
                    continuation.resume(value = false, onCancellation = null)
                }
        }


    // get notification setting for current user
    suspend fun getNotificationsSettingsUpdates(): Flow<Boolean> = callbackFlow {
        val settingsCollection = firestore
            .collection(WORKMATES_COLLECTION)
            .document(user.email)
            .collection(SETTINGS)
            .document(NOTIFICATIONS_SETTINGS)

        val settingsListener = settingsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val updatedSetting = snapshot.getBoolean("accept") ?: return@addSnapshotListener
                trySend(updatedSetting)
            }
        }
        awaitClose { settingsListener.remove() }
    }
}