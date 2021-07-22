package fr.plopez.go4lunch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.plopez.go4lunch.data.repositories.ActivePageRepository
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory:ViewModelProvider.Factory {

    private val activePageRepository : ActivePageRepository = ActivePageRepository()

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)){
            return MainActivityViewModel(activePageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class : $modelClass")
    }

    companion object {
        // Dependency injection
        val INSTANCE = ViewModelFactory()
    }
}