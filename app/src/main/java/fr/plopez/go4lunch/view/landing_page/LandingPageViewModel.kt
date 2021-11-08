package fr.plopez.go4lunch.view.landing_page

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.view.landing_page.LandingPageViewModel.LandingPageViewAction.GoToPermissions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class LandingPageViewModel
@Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val coroutinesProvider: CoroutinesProvider
) : ViewModel() {

    private val viewActionMutableSharedFlow = MutableSharedFlow<LandingPageViewAction>(replay = 1)
    val viewActionLiveData : LiveData<LandingPageViewAction> =
        viewActionMutableSharedFlow.asLiveData(coroutinesProvider.ioCoroutineDispatcher)


    fun onLoginSuccessful(user: FirebaseUser) {
        viewModelScope.launch(coroutinesProvider.ioCoroutineDispatcher){
            firestoreRepository.addOrUpdateUserOnLogin(user)
            viewActionMutableSharedFlow.emit(GoToPermissions)
        }
    }

    sealed class LandingPageViewAction {
        object GoToPermissions : LandingPageViewAction()
    }
}