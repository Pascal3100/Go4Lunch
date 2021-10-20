package fr.plopez.go4lunch.utils

import androidx.annotation.ColorInt

data class CustomSnackbar2 private constructor(
    val message: String,
    @ColorInt val backgroundTintColorInt: Int
) {
    data class Builder(
        var message: String? = null,
        @ColorInt var backgroundTintColorInt: Int
    ) {

        fun message(message: String) = apply {
            this.message = message
        }

        fun backgroundTintColorInt(@ColorInt backgroundTintColorInt: Int) = this.apply {
            this.backgroundTintColorInt = backgroundTintColorInt
        }

        fun build() = CustomSnackbar2(
            requireNotNull(message) { "Provide 'message' in builder" },
            backgroundTintColorInt
        )
    }
}