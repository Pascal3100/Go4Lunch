package fr.plopez.go4lunch.utils

import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class DateTimeUtils @Inject constructor() {

    // Here we want Monday always = 1
    fun getCurrentDay(): Int = if (MONDAY.value == 2) {
        // Manage the difference between FR and US format
        var day = LocalDate.now().dayOfWeek.value
            if (day > 1) {
                day -= 1
            } else {
                day += 6
            }
            day
        } else {
            LocalDate.now().dayOfWeek.value
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