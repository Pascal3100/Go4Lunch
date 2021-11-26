package fr.plopez.go4lunch.data.model.restaurant

import com.google.gson.annotations.SerializedName

data class AutoCompleteQueryResult (

    @SerializedName("predictions") val predictions : List<Predictions>?,
    @SerializedName("status") val status : String?
)
data class MainTextMatchedSubstrings (

    @SerializedName("length") val length : Int?,
    @SerializedName("offset") val offset : Int?
)
data class MatchedSubstrings (

    @SerializedName("length") val length : Int?,
    @SerializedName("offset") val offset : Int?
)
data class Predictions (

    @SerializedName("description") val description : String?,
    @SerializedName("matched_substrings") val matched_substrings : List<MatchedSubstrings>?,
    @SerializedName("place_id") val place_id : String?,
    @SerializedName("reference") val reference : String?,
    @SerializedName("structured_formatting") val structured_formatting : StructuredFormatting?,
    @SerializedName("terms") val terms : List<Terms>?,
    @SerializedName("types") val types : List<String>?
)
data class StructuredFormatting (

    @SerializedName("main_text") val main_text : String?,
    @SerializedName("main_text_matched_substrings") val main_text_matched_substrings : List<MainTextMatchedSubstrings>?,
    @SerializedName("secondary_text") val secondary_text : String?
)
data class Terms (

    @SerializedName("offset") val offset : Int?,
    @SerializedName("value") val value : String?
)