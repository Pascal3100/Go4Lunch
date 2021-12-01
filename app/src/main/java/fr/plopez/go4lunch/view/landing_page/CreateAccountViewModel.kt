package fr.plopez.go4lunch.view.landing_page

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class CreateAccountViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val EMAIL_PATTERN =
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        private const val AT_LEAST_ONE_UPPERCASE_PATTERN = "[A-Z]"
        private const val AT_LEAST_ONE_NUMBER_PATTERN = "[0-9]"
        private const val AT_LEAST_ONE_SPECIAL_CHAR_PATTERN = "[\\$&+,:;=?@#|'<>.^*()%!-]"
    }

    val createAccountAuthorizerSingleLiveEvent = SingleLiveEvent<CreateAccountAuthorizer>()

    enum class Messages(
        @get:StringRes
        val messageResId: Int
    ) {
        EMAIL_BAD_FORMAT(messageResId = R.string.email_bad_format_message),
        PASSWORD_BAD_FORMAT(messageResId = R.string.password_bad_format_message),
        PASSWORD_TOO_SHORT(messageResId = R.string.password_too_short_message)
    }

    fun onSubmitEmailPassword(email: String, password: String) {
        if (!isEmailRightFormat(email)) {
            createAccountAuthorizerSingleLiveEvent.value =
                CreateAccountAuthorizer.StatusMessage(Messages.EMAIL_BAD_FORMAT.messageResId)
        } else if (!isPasswordLongEnough(password)) {
            createAccountAuthorizerSingleLiveEvent.value =
                CreateAccountAuthorizer.StatusMessage(Messages.PASSWORD_TOO_SHORT.messageResId)
        } else if (!isPasswordRightFormat(password)) {
            createAccountAuthorizerSingleLiveEvent.value =
                CreateAccountAuthorizer.StatusMessage(Messages.PASSWORD_BAD_FORMAT.messageResId)
        } else {
            createAccountAuthorizerSingleLiveEvent.value =
                CreateAccountAuthorizer.Authorized
        }
    }

    private fun isEmailRightFormat(email: String): Boolean {
        val pattern = EMAIL_PATTERN.toRegex()
        return pattern.matches(email)
    }

    private fun isPasswordRightFormat(password: String) =
        false !in listOf(
            AT_LEAST_ONE_UPPERCASE_PATTERN,
            AT_LEAST_ONE_NUMBER_PATTERN,
            AT_LEAST_ONE_SPECIAL_CHAR_PATTERN
        ).map {
            it.toRegex().containsMatchIn(password)
        }

    private fun isPasswordLongEnough(password: String): Boolean = password.length >= 8

    sealed class CreateAccountAuthorizer {
        data class StatusMessage(@StringRes val messageResId: Int) : CreateAccountAuthorizer()
        object Authorized : CreateAccountAuthorizer()
    }
}