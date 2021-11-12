package fr.plopez.go4lunch.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import fr.plopez.go4lunch.data.model.restaurant.Workmate
import fr.plopez.go4lunch.utils.DateTimeUtils
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
    private val firebaseAuth: FirebaseAuth,
    private val dateTimeUtils: DateTimeUtils,
) {

    companion object {
        private const val WORKMATES_COLLECTION = "workmates"
        private const val LIKED_RESTAURANTS_COLLECTION = "liked_restaurants"
        private const val DATES_COLLECTION = "dates"
        private const val INTERESTED_WORKMATES_COLLECTION = "interested_workmates"
        private const val EMAIL_NAME_PATTERN = "(.*)@.*"
    }

    suspend fun addOrUpdateUserOnLogin() =
        suspendCancellableCoroutine<Boolean> { continuation ->
            // _!! allowed because if an error occurs it will be managed in addOnFailureListener
            val user = firebaseAuth.currentUser!!
            firestore.collection(WORKMATES_COLLECTION)
                .document(user.email.toString())
                .set(
                    Workmate(
                        name = user.displayName,
                        email = user.email.toString(),
                        photoUrl = user.photoUrl?.toString()
                    ), SetOptions.merge()
                )
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
                    Workmate(
                        name = it.getString("name")
                            ?: Regex(EMAIL_NAME_PATTERN).find(it.getString("email")!!)?.groupValues?.get(
                                1
                            ),
                        email = it.getString("email")!!,
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
        val workmateId = firebaseAuth.currentUser?.email
        val likedRestaurantsCollection = firestore
            .collection(WORKMATES_COLLECTION)
            .document(workmateId!!)
            .collection(LIKED_RESTAURANTS_COLLECTION)

        val likedRestaurantsListener =
            likedRestaurantsCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val likedRestaurantsList = snapshot.mapNotNull {
                        it.getString("id")
                    }

                    trySend(likedRestaurantsList)
                }
            }

        awaitClose { likedRestaurantsListener.remove() }
    }

    // Manage the liked status of the restaurant
    suspend fun addOrSuppressLikedRestaurant(placeId: String, likeState: Boolean) =
        suspendCancellableCoroutine<Boolean> { continuation ->
            val user = firebaseAuth.currentUser!!
            if (!likeState) {
                firestore.collection(WORKMATES_COLLECTION)
                    .document(user.email.toString())
                    .collection(LIKED_RESTAURANTS_COLLECTION)
                    .document(placeId)
                    .set(
                        hashMapOf(
                            "id" to placeId
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
                    .document(user.email.toString())
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
        val user = firebaseAuth.currentUser!!
        Log.d("TAG", "#### setOrUnsetSelectedRestaurant: selectedState = $selectedState")

        return suspendCancellableCoroutine { continuation ->
            if (!selectedState) {
                firestore
                    .collection(DATES_COLLECTION)
                    .document(dateTimeUtils.getCurrentDate())
                    .collection(INTERESTED_WORKMATES_COLLECTION)
                    .document(user.email.toString())
                    .set(
                        WorkmateWithSelectedRestaurant(
                            workmateName = user.displayName.toString(),
                            workmateEmail = user.email.toString(),
                            workmatePhotoUrl = user.photoUrl.toString(),
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
                    .document(user.email.toString())
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
                            WorkmateWithSelectedRestaurant(
                                workmateName = if (it.getString("workmateName") == "null" || it.getString("workmateName") == null) {
                                    Regex(EMAIL_NAME_PATTERN).find(it.getString("workmateEmail")!!)?.groupValues?.get(1)!!
                                } else {
                                    it.getString("workmateName")!!
                                },
                                workmateEmail = it.getString("workmateEmail")!!,
                                workmatePhotoUrl = it.getString("workmatePhotoUrl")!!,
                                selectedRestaurantName = it.getString("selectedRestaurantName")!!,
                                selectedRestaurantId = it.getString("selectedRestaurantId")!!
                            )
                        }
                        trySend(interestedWorkmatesWithSelectedRestaurantList)
                    }
                }

            awaitClose { selectedRestaurantsListener.remove() }
        }


}