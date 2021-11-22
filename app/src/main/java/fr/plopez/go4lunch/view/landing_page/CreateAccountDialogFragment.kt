package fr.plopez.go4lunch.view.landing_page

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.viewModelScope
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.*
import dagger.hilt.android.AndroidEntryPoint
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.databinding.CreateAccountDialogFragmentBinding
import fr.plopez.go4lunch.interfaces.OnLoginSuccessful
import fr.plopez.go4lunch.utils.CustomSnackBar
import fr.plopez.go4lunch.utils.exhaustive
import java.lang.ClassCastException
import javax.inject.Inject

import java.lang.Exception


@AndroidEntryPoint
class CreateAccountDialogFragment : DialogFragment() {

    companion object {
        private const val TAG = "CreateAccountDialog"
        fun newInstance() = CreateAccountDialogFragment()
    }

    private val createAccountViewModel: CreateAccountViewModel by viewModels()

    // Firebase Auth
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    // Interface
    private lateinit var onLoginSuccessful: OnLoginSuccessful

    // View binding
    private lateinit var binding: CreateAccountDialogFragmentBinding

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CreateAccountDialogFragmentBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.createAccountEmailLoginButton.setOnClickListener {
            createAccountViewModel.onSubmitEmailPassword(
                email = binding.createAccountEmailValue.text.toString(),
                password = binding.createAccountPasswordValue.text.toString(),
            )
        }

        createAccountViewModel.createAccountAuthorizerSingleLiveEvent.observe(this) {
            when (it) {
                is CreateAccountViewModel.CreateAccountAuthorizer.StatusMessage ->
                    CustomSnackBar.with(requireView())
                        .setMessage(getString(it.messageResId))
                        .setType(CustomSnackBar.Type.ERROR)
                        .build()
                        .show()
                is CreateAccountViewModel.CreateAccountAuthorizer.Authorized ->
                    createAccount(
                        email = binding.createAccountEmailValue.text.toString(),
                        password = binding.createAccountPasswordValue.text.toString()
                    )
            }
        }.exhaustive
    }

    private fun createAccount(email: String, password: String){
        firebaseAuth.createUserWithEmailAndPassword(
            email, password
        ).addOnCompleteListener(requireActivity()) { task ->
            if (!task.isSuccessful) {
                try {
                    throw task.exception!!
                } // if user already exist in base.
                 catch (existEmail: FirebaseAuthUserCollisionException) {
                     CustomSnackBar.with(requireView())
                         .setMessage(getString(R.string.email_already_exists))
                         .setType(CustomSnackBar.Type.ERROR)
                         .build()
                         .show()
                 } catch (existEmail: FirebaseAuthWeakPasswordException) {
                     CustomSnackBar.with(requireView())
                         .setMessage(getString(R.string.password_too_weak))
                         .setType(CustomSnackBar.Type.ERROR)
                         .build()
                         .show()
                } catch (e: Exception) {
                    CustomSnackBar.with(requireView())
                        .setMessage(getString(R.string.email_account_creation_failed))
                        .setType(CustomSnackBar.Type.ERROR)
                        .build()
                        .show()
                }

            } else {
                onLoginSuccessful.onLoginSuccessful()
            }
        }
    }

}