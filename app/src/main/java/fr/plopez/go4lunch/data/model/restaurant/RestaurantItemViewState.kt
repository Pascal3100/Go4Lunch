package fr.plopez.go4lunch.data.model.restaurant

data class RestaurantItemViewState(
    val name: String,
    val address: String,
    val openingStateText: String,
    val distanceToUser: String,
    val numberOfInterestedWorkmates: Int,
    val rate: String,
    val photoUrl: String
)
