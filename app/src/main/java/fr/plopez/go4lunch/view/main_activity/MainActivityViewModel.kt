package fr.plopez.go4lunch.view.main_activity

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.utils.FirebaseAuthUtils
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel.MainActivityViewAction.NoRestaurantSelected
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel.MainActivityViewAction.SelectedRestaurant
import fr.plopez.go4lunch.view.main_activity.SearchUseCase.SearchStringStatus.EmptyString
import fr.plopez.go4lunch.view.main_activity.SearchUseCase.SearchStringStatus.SearchString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val searchUseCase: SearchUseCase,
    coroutinesProvider: CoroutinesProvider,
    firebaseAuthUtils: FirebaseAuthUtils
) : ViewModel() {

    private val wantToSeeMyRestaurantMutableLiveData = MutableLiveData<Unit>()
    val selectedRestaurantIdLiveData: LiveData<MainActivityViewAction>
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
                        } else {
                            emit(SelectedRestaurant(it.selectedRestaurantId))
                        }
                    }
            }
        }
    }

    fun onWantToSeeMyRestaurant() {
        wantToSeeMyRestaurantMutableLiveData.value = Unit
    }

    fun onSearchTextChange(newSearchText: String?) {
        if (newSearchText != null && newSearchText.length > 2) {
            searchUseCase.updateSearchText(SearchString(data = newSearchText))
        } else {
            searchUseCase.updateSearchText(EmptyString)
        }
    }

    fun isWorkmatesView(isWorkmatesViewDisplayed : Boolean) {
        searchUseCase.updateWorkmatesViewDisplayState(isWorkmatesViewDisplayed)
    }

    sealed class MainActivityViewAction {
        object NoRestaurantSelected : MainActivityViewAction()
        data class SelectedRestaurant(val id: String) : MainActivityViewAction()
    }
}