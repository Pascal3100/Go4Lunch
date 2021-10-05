package fr.plopez.go4lunch.data.model.restaurant

data class DetailsQueryResult(
    val html_attributions: List<Any>?,
    val result: Result?,
    val status: String?
)

data class Result(
    val opening_hours: OpeningHours?
)

data class OpeningHours(
    val open_now: Boolean?,
    val periods: List<Period>?,
    val weekday_text: List<String>?
)

data class Period(
    val close: Close?,
    val open: Open?
)

data class Open(
    val day: Int?,
    val time: String?
)

data class Close(
    val day: Int?,
    val time: String?
)