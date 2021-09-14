package fr.plopez.go4lunch.utils

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.view.main_activity.MainActivity

class CustomSnackBar constructor(private val parentView: View) {

    private lateinit var snackbar: Snackbar

    private fun initSnackBar(message: String) {
        snackbar =
    }

    fun showNormalSnackBar(message): String {
        initSnackBar(message)

        snackbar.setBackgroundTint(ContextCompat.getColor(parentView.context, R.color.grey))
        snackbar.setTextColor(ContextCompat.getColor(parentView.context, R.color.white))

        snackbar.show()
    }

    fun showWarningSnackBar(message: String) {
        initSnackBar(message)

        snackbar.setBackgroundTint(ContextCompat.getColor(parentView.context, R.color.yellow_warning))
        snackbar.setTextColor(ContextCompat.getColor(parentView.context, R.color.light_grey))

        snackbar.show()
    }

    fun showErrorSnackBar(message: String) {
        initSnackBar(message)

        snackbar.setBackgroundTint(ContextCompat.getColor(parentView.context, R.color.redish_whisky))
        snackbar.setTextColor(ContextCompat.getColor(parentView.context, R.color.white))

        snackbar.show()
    }

}