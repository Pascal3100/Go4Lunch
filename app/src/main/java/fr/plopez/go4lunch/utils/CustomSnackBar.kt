package fr.plopez.go4lunch.utils

import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import fr.plopez.go4lunch.R

class CustomSnackBar {

    companion object {
        fun with(view: View) = Builder(view)
    }

    enum class Type(
        @get:ColorRes
        val backgroundTintColorInt: Int,
        @get:ColorRes
        val textColorInt: Int
    ) {
        WARNING(backgroundTintColorInt = R.color.yellow_warning, textColorInt = R.color.white),
        ERROR(backgroundTintColorInt = R.color.redish_whisky, textColorInt = R.color.white),
        DEFAULT(backgroundTintColorInt = R.color.grey, textColorInt = R.color.white)
    }

    class Builder(private val view: View) {

        private var message: String? = null

        @ColorRes
        private var backgroundTintColorInt: Int = Type.DEFAULT.backgroundTintColorInt

        @ColorRes
        private var textColorInt: Int = Type.DEFAULT.textColorInt

        fun setMessage(message: String) = apply {
            this.message = message
        }

        fun setType(type: Type) = apply {
            backgroundTintColorInt = type.backgroundTintColorInt
            textColorInt = type.textColorInt
        }

        fun build(): Snackbar = Snackbar.make(
            view,
            requireNotNull(message, { "require not null message" }),
            Snackbar.LENGTH_SHORT
        ).apply {
            setBackgroundTint(ContextCompat.getColor(view.context, backgroundTintColorInt))
            setTextColor(ContextCompat.getColor(view.context, textColorInt))
        }
    }
}
