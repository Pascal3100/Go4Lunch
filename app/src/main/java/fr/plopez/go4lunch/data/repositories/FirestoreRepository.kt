package fr.plopez.go4lunch.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import fr.plopez.go4lunch.data.model.restaurant.Workmate
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
    private val firebaseAuth: FirebaseAuth
) {

    companion object {
        private const val WORKMATES_COLLECTION = "workmates"
        private const val LIKED_RESTAURANTS_COLLECTION = "liked_restaurants"
        private const val DATES_COLLECTION = "dates"
        private const val RESTAURANTS_IDS_COLLECTION = "restaurants_ids"
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
                        name = if (it.getString("name") == "null") null else it.getString("name"),
                        email = it.getString("email")!!,
                        photoUrl = if (it.getString("photoUrl") == "null") null else it.getString("photoUrl")
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

    suspend fun addOrSuppressLikedRestaurant(placeId: String?, likeState: Boolean?) =
        suspendCancellableCoroutine<Boolean> { continuation ->
            val user = firebaseAuth.currentUser!!
            if (!likeState!!) {
                firestore.collection(WORKMATES_COLLECTION)
                    .document(user.email.toString())
                    .collection(LIKED_RESTAURANTS_COLLECTION)
                    .document(placeId!!)
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
                    .document(placeId!!)
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