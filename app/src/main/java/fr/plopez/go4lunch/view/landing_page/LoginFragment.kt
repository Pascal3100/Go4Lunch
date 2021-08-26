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
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.FragmentLoginBinding
import fr.plopez.go4lunch.utils.CustomSnackBar
import com.google.firebase.auth.FirebaseUser
import fr.plopez.go4lunch.interfaces.OnLoginSuccessful
import java.lang.ClassCastException


class LoginFragment : Fragment() {

    companion object {
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }

        private val TAG = "LoginFragment"

        // Interface
        private lateinit var onLoginSuccessful: OnLoginSuccessful

        // Google authentication code
        private val GOOGLE_AUTH_REQUEST_CODE = 999

        // Loading state
        private val IS_LOADING_STATE = "IS_LOADING_STATE"
    }

    private lateinit var snack : CustomSnackBar

    // Loading state
    private var isLoading = false

    // View binding
    private lateinit var binding: FragmentLoginBinding

    // Facebook callManager
    private lateinit var callbackManager : CallbackManager

    // Firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth

    // ViewModel
    private val landingPageViewModel = LandingPageViewModel()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLoginSuccessful) {
            onLoginSuccessful = context as OnLoginSuccessful
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

        //Initialize custom snackBar
        snack = CustomSnackBar(requireView(), requireContext())

        // Initialize Facebook call manager
        callbackManager = CallbackManager.Factory.create()

        // Initialize Firebase Auth
        firebaseAuth = Firebase.auth

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
                snack.showWarningSnackBar("Facebook login cancelled")
            }

            override fun onError(error: FacebookException?) {
                snack.showErrorSnackBar("Facebook login failed: ${error.toString()}")
            }
        })
    }

    // FaceBook authentication on Firebase
    private fun firebaseAuthWithFacebook(token: AccessToken){
        val credential :AuthCredential = FacebookAuthProvider.getCredential(token.token)
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
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    loading(false)
                    // Notify viewModel
                    landingPageViewModel.setUserInformation(firebaseAuth.currentUser)

                    onLoginSuccessful.onLoginSuccessful(true)

                } else {
                    loading(false)
                    snack.showWarningSnackBar("Hey, you're not connected")
                    onLoginSuccessful.onLoginSuccessful(false)
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
                snack.showErrorSnackBar("Google sign in failed.")
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