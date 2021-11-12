package fr.plopez.go4lunch.view.model

data class RestaurantDetailsViewState (
    val photoUrl: String,
    val name: String,
    val address: String,
    val rate: Float,
    val phoneNumber: String,
    val website: String,
    val isSelected : Boolean,
    val isFavorite : Boolean,
    val interestedWorkmatesList: List<WorkmateViewState>
)