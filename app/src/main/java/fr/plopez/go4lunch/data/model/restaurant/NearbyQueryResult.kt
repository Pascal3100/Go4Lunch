package fr.plopez.go4lunch.data.model.restaurant

import com.google.gson.annotations.SerializedName

data class NearbyQueryResult(
    val html_attributions: List<Any>,
    val next_page_token: String,
    val results: List<RestaurantQueryResponseItem>,
    val status: String
)

data class RestaurantQueryResponseItem (
    @SerializedName("business_status")
    val businessStatus: String?,

    @SerializedName("geometry")
    val geometry: Geometry?,

    @SerializedName("icon")
    val icon: String?,

    @SerializedName("icon_background_color")
    val iconBackgroundColor: String?,

    @SerializedName("icon_mask_base_uri")
    val iconMaskBaseURI: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("photos")
    val photos: List<Photo>?,

    @SerializedName("place_id")
    val placeID: String?,

    @SerializedName("rating")
    val rating: Double?,

    @SerializedName("reference")
    val reference: String?,

    @SerializedName("scope")
    val scope: String?,

    @SerializedName("types")
    val types: List<String>?,

    @SerializedName("user_ratings_total")
    val userRatingsTotal: Long?,

    @SerializedName("vicinity")
    val vicinity: String?
)

data class Geometry (
    @SerializedName("location")
    val location: Location?,

    @SerializedName("viewport")
    val viewport: Viewport?
)

data class Location (
    @SerializedName("lat")
    val lat: Double?,

    @SerializedName("lng")
    val lng: Double?
)

data class Viewport (
    @SerializedName("northeast")
    val northeast: Location?,

    @SerializedName("southwest")
    val southwest: Location?
)

data class Photo (
    @SerializedName("height")
    val height: Long?,

    @SerializedName("html_attributions")
    val htmlAttributions: List<String>?,

    @SerializedName("photo_reference")
    val photoReference: String?,

    @SerializedName("width")
    val width: Long?
)

