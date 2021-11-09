package fr.plopez.go4lunch.view.landing_page

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.view.landing_page.LandingPageViewModel.LandingPageViewAction.GoToPermissions
import fr.plopez.go4lunch.view.landing_page.LandingPageViewModel.LandingPageViewAction.FirestoreFails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class LandingPageViewModel
@Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val coroutinesProvider: CoroutinesProvider
) : ViewModel() {

    private val viewActionMutableLiveData = MutableLiveData<LandingPageViewAction>()
    val viewActionLiveData : LiveData<LandingPageViewAction> = viewActionMutableLiveData


    fun onLoginSuccessful() {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher){

            val success = firestoreRepository.addOrUpdateUserOnLogin()

            withContext(coroutinesProvider.mainCoroutineDispatcher){
                if (success) {
                    viewActionMutableLiveData.value = GoToPermissions
                } else {
                    viewActionMutableLiveData.value = FirestoreFails
                }
            }
        }
    }

    sealed class LandingPageViewAction {
        object GoToPermissions : LandingPageViewAction()
        object FirestoreFails : LandingPageViewAction()
    }
}