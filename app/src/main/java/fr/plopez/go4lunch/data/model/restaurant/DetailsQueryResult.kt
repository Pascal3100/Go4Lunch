package fr.plopez.go4lunch.data.model.restaurant

//import com.google.gson.annotations.SerializedName

data class DetailsQueryResult(
//    @SerializedName("html_attributions")
    val html_attributions: List<Any>?,
//    @SerializedName("result")
    val result: Result?,
//    @SerializedName("status")
    val status: String?
)

data class Result(
//    @SerializedName("opening_hours")
    val opening_hours: OpeningHours?,
//    @SerializedName("international_phone_number")
    val phone_number: String,
//    @SerializedName("website")
    val website: String
)

data class OpeningHours(
//    @SerializedName("open_now")
    val open_now: Boolean?,
//    @SerializedName("website")
    val periods: List<Period>?,
//    @SerializedName("weekday_text")
    val weekday_text: List<String>?
)

data class Period(
//    @SerializedName("close")
    val close: Close?,
//    @SerializedName("open")
    val open: Open?
)

data class Open(
//    @SerializedName("day")
    val day: Int?,
//    @SerializedName("time")
    val time: String?
)

data class Close(
//    @SerializedName("day")
    val day: Int?,
//    @SerializedName("time")
    val time: String?
)