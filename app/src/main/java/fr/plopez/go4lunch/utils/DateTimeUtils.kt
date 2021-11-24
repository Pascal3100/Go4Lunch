package fr.plopez.go4lunch.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject

class DateTimeUtils @Inject constructor() {

    fun getCurrentDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    fun getCurrentTime(): LocalTime = LocalTime.now()

    fun getCurrentDate(): String = LocalDate.now().toString()

    fun getDelayUtilLunch(): Long {
        val now = getCurrentTime()
        val lunchTime = LocalTime.NOON

        return if (now.isBefore(lunchTime)) {
            now.until(lunchTime, ChronoUnit.MILLIS)
        } else {
            0L
        }
    }





}