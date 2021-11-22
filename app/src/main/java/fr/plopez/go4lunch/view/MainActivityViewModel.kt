package fr.plopez.go4lunch.view

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.utils.FirebaseAuthUtils
import fr.plopez.go4lunch.utils.SingleLiveEvent
import fr.plopez.go4lunch.view.MainActivityViewModel.MainActivityViewAction.NoRestaurantSelected
import fr.plopez.go4lunch.view.MainActivityViewModel.MainActivityViewAction.SelectedRestaurant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    coroutinesProvider: CoroutinesProvider,
    firebaseAuthUtils: FirebaseAuthUtils
) : ViewModel() {

    private val wantToSeeMyRestaurantMutableLiveData = MutableLiveData<Unit>()
    val selectedRestaurantIdLiveData : LiveData<MainActivityViewAction>
    private val user = firebaseAuthUtils.getUser()

    init {
        selectedRestaurantIdLiveData = wantToSeeMyRestaurantMutableLiveData.switchMap {
            liveData(coroutinesProvider.ioCoroutineDispatcher) {
                firestoreRepository.getWorkmatesWithSelectedRestaurants()
                    .map { workmateWithSelectedRestaurantList ->
                        workmateWithSelectedRestaurantList.firstOrNull { it.workmateEmail == user.email }
                    }.collect {
                        if (it == null) {
                            emit(NoRestaurantSelected)
                        }
                        else {
                            emit(SelectedRestaurant(it.selectedRestaurantId))
                        }
                    }
            }
        }
    }

    fun onWantToSeeMyRestaurant() {
        wantToSeeMyRestaurantMutableLiveData.value = Unit
    }

    sealed class MainActivityViewAction {
        object NoRestaurantSelected : MainActivityViewAction()
        data class SelectedRestaurant(val id: String) : MainActivityViewAction()
    }
}