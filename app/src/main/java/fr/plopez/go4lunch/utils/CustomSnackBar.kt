package fr.plopez.go4lunch.utils

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import fr.plopez.go4lunch.R

class CustomSnackBar constructor(private val parentView: View,
                                 private val parentContext: Context) {

    private lateinit var snackbar: Snackbar

    private fun initSnackBar(message: String) {
        snackbar = Snackbar.make(
            parentView,
            message,
            Snackbar.LENGTH_SHORT
        )
    }

    fun showNormalSnackBar(message: String) {
        initSnackBar(message)

        snackbar.setBackgroundTint(ContextCompat.getColor(parentContext, R.color.grey))
        snackbar.setTextColor(ContextCompat.getColor(parentContext, R.color.white))

        snackbar.show()
    }

    fun showWarningSnackBar(message: String) {
        initSnackBar(message)

        snackbar.setBackgroundTint(ContextCompat.getColor(parentContext, R.color.yellow_warning))
        snackbar.setTextColor(ContextCompat.getColor(parentContext, R.color.light_grey))

        snackbar.show()
    }

    fun showErrorSnackBar(message: String) {
        initSnackBar(message)

        snackbar.setBackgroundTint(ContextCompat.getColor(parentContext, R.color.redish_whisky))
        snackbar.setTextColor(ContextCompat.getColor(parentContext, R.color.white))

        snackbar.show()
    }

}