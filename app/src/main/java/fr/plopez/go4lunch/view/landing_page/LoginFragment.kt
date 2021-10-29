package fr.plopez.go4lunch.view.landing_page

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.FragmentLoginBinding
import fr.plopez.go4lunch.utils.CustomSnackBar
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.interfaces.OnLoginSuccessful
import java.lang.ClassCastException
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    companion object {
        fun newInstance() = LoginFragment()

        private const val TAG = "LoginFragment"

        // Interface
        private lateinit var onLoginSuccessful: OnLoginSuccessful

        // Google authentication code
        private const val GOOGLE_AUTH_REQUEST_CODE = 999

        // Loading state
        private const val IS_LOADING_STATE = "IS_LOADING_STATE"
    }

    // Loading state
    private var isLoading = false

    // View binding
    private lateinit var binding: FragmentLoginBinding

    // Facebook callManager
    @Inject
    lateinit var callbackManager: CallbackManager

    // Firebase Auth
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    // ViewModel provided by delegate
    private val landingPageViewModel by viewModels<LandingPageViewModel>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLoginSuccessful) {
            onLoginSuccessful = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement OnLoginSuccessful"
            )
        }
    }

    override fun onCreateView(
        layoutInflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        binding = FragmentLoginBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Restore the previous loading state or initialize it
        if (savedInstanceState != null) {
            loading(savedInstanceState.getBoolean(IS_LOADING_STATE, false))
        } else {
            loading(false)
        }

        // ------------------- Facebook authentication -------------------
        val facebookButton = binding.loginFragmentFacebookLoginButton
        facebookButton.setOnClickListener { loginWithFacebook() }
        // ---------------------------------------------------------------

        // ------------------- Google authentication -------------------
        val googleButton = binding.loginFragmentGoogleLoginButton
        googleButton.setOnClickListener { loginWithGoogle() }
        // ---------------------------------------------------------------

        val layout = binding.loginContainer
        val button = binding.loginFragmentEmailLoginButtonSelection
        val group = binding.loginFragmentEmailLoginGroup

        // ------------------- Email authentication -------------------
        // Manage login with email display
        button.setOnClickListener {
            // Transition to show login with email stuff when button clicked
            Log.d(TAG, "onViewCreated: pass")
            TransitionManager.beginDelayedTransition(layout)
            // Clears the selection button
            it.isVisible = false
            group.isVisible = true
            binding.loginFragmentScrollView.smoothScrollTo(
                0,
                binding.loginFragmentScrollView.bottom
            )
        }

        binding.loginFragmentEmailLoginButton.setOnClickListener {
            loginWithEmail(
                email = binding.loginFragmentEmailValue.text.toString(),
                password = binding.loginFragmentPasswordValue.text.toString()
            )
        }

        binding.loginFragmentForgotPasswordLink.setOnClickListener {
            CustomSnackBar.with(requireView())
                .setMessage("Forgot password???")
                .setType(CustomSnackBar.Type.DEFAULT)
                .build()
                .show()
        }

        binding.loginFragmentCreateAccountLink.setOnClickListener {
            CreateAccountDialogFragment.newInstance()
                .show(requireActivity().supportFragmentManager, "")
        }

        // ---------------------------------------------------------------
    }

    // Save the loading state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_LOADING_STATE, isLoading)
    }

    // FaceBook authentication
    private fun loginWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, setOf("email"))
        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                loading(true)
                // Facebook authentication successful
                firebaseAuthWithFacebook(result?.accessToken!!)
            }

            override fun onCancel() {
                CustomSnackBar.with(requireView())
                    .setMessage(getString(R.string.facebook_canceled_connection_message))
                    .setType(CustomSnackBar.Type.WARNING)
                    .build()
                    .show()
            }

            override fun onError(error: FacebookException?) {
                CustomSnackBar.with(requireView())
                    .setMessage(getString(R.string.facebook_connection_failed_message))
                    .setType(CustomSnackBar.Type.ERROR)
                    .build()
                    .show()
            }
        })
    }

    // FaceBook authentication on Firebase
    private fun firebaseAuthWithFacebook(token: AccessToken) {
        val credential: AuthCredential = FacebookAuthProvider.getCredential(token.token)
        signInToFirebase(credential)
    }

    // Google authentication
    private fun loginWithGoogle() {

        // Configure Google Sign In
        val googleSignInOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient =
            GoogleSignIn.getClient(requireActivity(), googleSignInOptions)

        // Launch Google Sign In
        loading(true)
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent, GOOGLE_AUTH_REQUEST_CODE)
    }

    // Google authentication on Firebase
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        signInToFirebase(credential)
    }

    // Firebase authentication
    private fun signInToFirebase(credential: AuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    loading(false)

                    // Notify viewModel
                    landingPageViewModel.connectedUser(firebaseAuth.currentUser)

                    onLoginSuccessful.onLoginSuccessful(true)

                } else {
                    loading(false)
                    CustomSnackBar.with(requireView())
                        .setMessage(getString(R.string.not_connected_message))
                        .setType(CustomSnackBar.Type.WARNING)
                        .build()
                        .show()

                    // Notify viewModel
                    landingPageViewModel.connectedUser(null)

                    onLoginSuccessful.onLoginSuccessful(false)
                }
            }
    }

    // Email/password authentication
    private fun loginWithEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Notify viewModel
                    onLoginSuccessful.onLoginSuccessful(true)
                } else {
                    // Notify viewModel
                    onLoginSuccessful.onLoginSuccessful(false)

                    try {
                        throw task.exception!!
                    } // if user not exist in base.
                    catch (invalidUser: FirebaseAuthInvalidUserException) {
                        CustomSnackBar.with(requireView())
                            .setMessage(getString(R.string.invalid_email))
                            .setType(CustomSnackBar.Type.ERROR)
                            .build()
                            .show()
                    } catch (existEmail: FirebaseAuthInvalidCredentialsException) {
                        CustomSnackBar.with(requireView())
                            .setMessage(getString(R.string.invalid_password))
                            .setType(CustomSnackBar.Type.ERROR)
                            .build()
                            .show()
                    }
                }
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Google
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_AUTH_REQUEST_CODE) {

            loading(true)

            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

                firebaseAuthWithGoogle(account)

            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                loading(false)
                CustomSnackBar.with(requireView())
                    .setMessage(getString(R.string.google_connection_failed_message))
                    .setType(CustomSnackBar.Type.ERROR)
                    .build()
                    .show()

            }
        }
    }

    // Display loading indicator utility
    private fun loading(isLoading: Boolean) {
        this.isLoading = isLoading
        val indicator = binding.loginFragmentProgressIndicator

        if (isLoading) {
            indicator.visibility = View.VISIBLE
        } else {
            indicator.visibility = View.GONE
        }
    }

}