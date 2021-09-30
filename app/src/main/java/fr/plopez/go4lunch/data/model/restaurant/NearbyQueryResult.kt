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

    val geometry: Geometry?,
    val icon: String?,

    @SerializedName("icon_background_color")
    val iconBackgroundColor: String?,

    @SerializedName("icon_mask_base_uri")
    val iconMaskBaseURI: String?,

    val name: String?,

//    @SerializedName("opening_hours")
//    val openingHours: OpeningHours,

    val photos: List<Photo>?,

    @SerializedName("place_id")
    val placeID: String?,

    @SerializedName("plus_code")
    val plusCode: PlusCode?,

    val rating: Double?,
    val reference: String?,
    val scope: String?,
    val types: List<String>?,

    @SerializedName("user_ratings_total")
    val userRatingsTotal: Long?,

    val vicinity: String?
)

data class Geometry (
    val location: Location,
    val viewport: Viewport
)

data class Location (
    val lat: Double,
    val lng: Double
)

data class Viewport (
    val northeast: Location,
    val southwest: Location
)

//data class OpeningHours (
//    @SerializedName("open_now")
//    val openNow: Boolean
//)

data class Photo (
    val height: Long,

    @SerializedName("html_attributions")
    val htmlAttributions: List<String>,

    @SerializedName("photo_reference")
    val photoReference: String,

    val width: Long
)

data class PlusCode (
    @SerializedName("compound_code")
    val compoundCode: String,

    @SerializedName("global_code")
    val globalCode: String
)
