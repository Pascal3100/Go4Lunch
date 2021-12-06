package fr.plopez.go4lunch.view.main_activity.list_restaurants

data class RestaurantItemViewState(
    val name: String,
    val address: String,
    val openingStateText: String,
    val distanceToUser: String,
    val numberOfInterestedWorkmates: String,
    val rate: Float,
    val photoUrl: String,
    val id: String
)
