package fr.plopez.go4lunch.tests.utils

import fr.plopez.go4lunch.data.model.restaurant.Close
import fr.plopez.go4lunch.data.model.restaurant.Open
import fr.plopez.go4lunch.data.model.restaurant.Period
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.di.NearbyConstants
import java.time.LocalTime

object CommonsUtils {
    const val LATITUDE = "90.0"
    const val LONGITUDE = "0.0"
    const val ZOOM = "15.0"
    const val RADIUS = "1000"

    const val PLACE_ID = "PLACE_ID"
    const val NAME = "NAME"
    const val ADDRESS = "ADDRESS"
    const val PHOTO_URL = "PHOTO_URL"
    const val RATE = "5.0"
    const val PHONE_NUMBER = "+33 0 00 00 00 00"
    const val WEBSITE = "www.no-web-site.com"

    const val QUERY_TIME_STAMP = 66666666L
    const val LOCATION = "$LATITUDE,$LONGITUDE"
    const val MAX_DISPLACEMENT_TOL = "0.000449"
    const val PERIODS_SEARCH_FIELD = "opening_hours,international_phone_number,website"
    const val ERROR_CODE = 404

    const val PHOTO_MAX_WIDTH = "1080"
    const val GOOGLE_PHOTOS_API_URL = "https://maps.googleapis.com/maps/api/place/photo?"

    private val nearbyConstants = NearbyConstants()

    fun getDefaultRestaurantOpeningPeriodList(
        periodList: List<Period> = getDefaultPeriodList()
    ) = periodList.map { period ->

        val day = period.open?.day!!
        val openingHour = period.open?.time!!
        val closingHour = period.close?.time!!
        val periodId = "$day$openingHour$closingHour"

        RestaurantOpeningPeriod(
            periodId,
            openingHour = LocalTime.of(
                openingHour.take(2).toInt(),
                openingHour.takeLast(2).toInt()
            ).toString(),
            closingHour = LocalTime.of(
                closingHour.take(2).toInt(),
                closingHour.takeLast(2).toInt()
            ).toString(),
            dayOfWeek = day
        )
    }


    fun getDefaultPeriodList(
        periodList: List<List<String>> = getDefaultHours()
    ) = (1..7).map { day ->
        periodList.map { time ->
            Period(
                Close(
                    day = day,
                    time = time[1]
                ),
                Open(
                    day = day,
                    time = time[0]
                )
            )
        }
    }.flatten()

    private fun getDefaultHours() =
        listOf(listOf("0800", "1200"), listOf("1400", "1830"))

    fun getDefaultRestaurantEntityList() = listOf(
        RestaurantEntity(
            restaurantId = PLACE_ID,
            name = NAME,
            address = ADDRESS,
            latitude = LATITUDE.toDouble(),
            longitude = LONGITUDE.toDouble(),
            photoUrl = PHOTO_URL,
            rate = RATE.toFloat(),
            phoneNumber = PHONE_NUMBER,
            website = WEBSITE
        )
    )

    fun getDefaultPhotoUrl(
        photoUrl: String = PHOTO_URL
    )= GOOGLE_PHOTOS_API_URL+
            "maxwidth=${PHOTO_MAX_WIDTH}&" +
            "photoreference=$photoUrl&" +
            "key=${nearbyConstants.key}"
}