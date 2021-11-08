package fr.plopez.go4lunch.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.SetOptions
import fr.plopez.go4lunch.data.model.restaurant.Workmate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
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

    private val likeStateMutableStateFlow = MutableStateFlow(false)
    val likeStateFlow:Flow<Boolean> = likeStateMutableStateFlow

    fun addOrUpdateUserOnLogin(user: FirebaseUser) {
        // we use add method here to auto generate document id
        firestore.collection(WORKMATES_COLLECTION)
            .document(user.email.toString())
            .set(Workmate(
                    name = user.displayName.toString(),
                    email = user.email.toString(),
                    photoUrl = user.photoUrl.toString()
                ), SetOptions.merge())
            .addOnFailureListener {
                Log.d("TAG", "addOrUpdateUserOnLogin: Problem sir! --> $it")
                // TODO is it necessary to manage it?
            }
    }

    // Listener for workmates collection updates
    suspend fun getWorkmatesUpdates() : Flow<List<Workmate>> = callbackFlow {
        val workmatesCollection = firestore.collection(WORKMATES_COLLECTION)

        val workmatesListener = workmatesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val updatedWorkmatesList = snapshot.mapNotNull {
                    Workmate(
                        name = it.getString("name")!!,
                        email = it.getString("email")!!,
                        photoUrl = it.getString("photoUrl")!!
                    )
                }
                trySend(updatedWorkmatesList)
            }
        }
        awaitClose { workmatesListener.remove() }
    }

    fun onLikeRestaurant(placeId: String?) {
        val workmateId = firebaseAuth.currentUser?.email
        val likedRestaurantsCollection = firestore
            .collection(WORKMATES_COLLECTION)
            .document(workmateId!!)
            .collection(LIKED_RESTAURANTS_COLLECTION)

//        if (placeId in likedRestaurantsCollection.)

    }


}