package fr.plopez.go4lunch.data.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoroutinesProvider @Inject constructor() {
    val mainCoroutineDispatcher: CoroutineDispatcher = Dispatchers.Main
    val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
}