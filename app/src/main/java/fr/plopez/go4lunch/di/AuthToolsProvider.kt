package fr.plopez.go4lunch.di

import com.facebook.CallbackManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.plopez.go4lunch.utils.FirebaseAuthUtils

@InstallIn(SingletonComponent::class)
@Module
class AuthToolsProvider {

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    fun provideFirebaseAuthUtils(): FirebaseAuthUtils = FirebaseAuthUtils()

    @Provides
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    fun provideFacebookCallBackManager(): CallbackManager {
        return CallbackManager.Factory.create()
    }
}
