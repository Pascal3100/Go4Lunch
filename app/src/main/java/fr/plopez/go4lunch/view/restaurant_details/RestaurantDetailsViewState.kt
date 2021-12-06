package fr.plopez.go4lunch.view.restaurant_details

import fr.plopez.go4lunch.view.model.WorkmateViewState

data class RestaurantDetailsViewState(
    val photoUrl: String,
    val id: String,
    val name: String,
    val address: String,
    val rate: Float,
    val phoneNumber: String,
    val website: String,
    val isSelected: Boolean,
    val isFavorite: Boolean,
    val interestedWorkmatesList: List<WorkmateViewState>,
    val currentUserEmail: String,
    val delay: Long
)