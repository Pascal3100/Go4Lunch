package fr.plopez.go4lunch.utils

import android.content.Context
import android.view.View
import androidx.annotation.ColorInt
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


class CustomSnackBar2 {
    data class Builder(
        var message: String? = null,
        @get:ColorInt
        var backgroundTintColorInt: Int = R.color.grey,
        @get:ColorInt
        var textColorInt: Int = R.color.white,

        var parentView: View,
        var parentContext: Context
    ) {
        fun setMessage(message: String) = apply {
            this.message = message
        }

        fun setMessageType(type: String) = apply {
            when (type) {
                "warning" -> {
                    backgroundTintColorInt = R.color.yellow_warning
                    textColorInt = R.color.white
                }
                "error" -> {
                    backgroundTintColorInt = R.color.redish_whisky
                    textColorInt = R.color.white
                }
                else -> {
                    backgroundTintColorInt = R.color.grey
                    textColorInt = R.color.white
                }
            }
        }

        fun setParentView(view: View) = apply {
            parentView = view
        }

        fun setParentContext(context: Context) = apply {
            parentContext = context
        }

        fun build() = Snackbar.make(
            parentView,
            requireNotNull(message, { "require not null message" }),
            Snackbar.LENGTH_SHORT
        ).setBackgroundTint(ContextCompat.getColor(parentContext, backgroundTintColorInt))
            .setTextColor(ContextCompat.getColor(parentContext, textColorInt))
            .show()
    }
}