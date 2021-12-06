package fr.plopez.go4lunch.tests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.plopez.go4lunch.data.User
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.data.di.CoroutinesProvider
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_EMAIL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_PHOTO_URL
import fr.plopez.go4lunch.tests.utils.LiveDataUtils.getOrAwaitValue
import fr.plopez.go4lunch.utils.FirebaseAuthUtils
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel.MainActivityViewAction.NoRestaurantSelected
import fr.plopez.go4lunch.view.main_activity.MainActivityViewModel.MainActivityViewAction.SelectedRestaurant
import fr.plopez.go4lunch.view.main_activity.SearchUseCase
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@FlowPreview
@ExperimentalCoroutinesApi
class MainActivityViewModelTest {

    companion object {
        const val OTHER_WORKMATE_NAME = "OTHER_WORKMATE_NAME"
        const val OTHER_WORKMATE_EMAIL = "OTHER_WORKMATE_ADDRESS"
        const val OTHER_WORKMATE_PHOTO_URL = "OTHER_WORKMATE_PHOTO_URL"

        const val LESS_THAN_3_CHAR = "TO"
        const val MORE_THAN_3_CHAR = "TOTO"
    }

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()
    private val firestoreRepositoryMockk = mockk<FirestoreRepository>()
    private val firebaseAuthUtilsMockk = mockk<FirebaseAuthUtils>()
    private val searchUseCaseMock = mockk<SearchUseCase>()

    //
    private val nullSearchString = null


    @Before
    fun setUp() {
        // Coroutines provider mock - provides a specific dispatcher for tests
        every { coroutinesProviderMock.ioCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher
        every { coroutinesProviderMock.mainCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher

        // Mock firestore repository
        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(
            listOf(
                WorkmateWithSelectedRestaurant(
                    workmateName = WORKMATE_NAME,
                    workmateEmail = WORKMATE_EMAIL,
                    workmatePhotoUrl = WORKMATE_PHOTO_URL,
                    selectedRestaurantName = PLACE_NAME,
                    selectedRestaurantId = PLACE_ID
                )
            )
        )

        // Mock Firebase Auth utils
        every { firebaseAuthUtilsMockk.getUser() } returns
                User(
                    name = WORKMATE_NAME,
                    email = WORKMATE_EMAIL,
                    photoUrl = WORKMATE_PHOTO_URL
                )

        // Mock searchUseCase
        justRun { searchUseCaseMock.updateSearchText(any()) }
        justRun { searchUseCaseMock.updateWorkmatesViewDisplayState(any()) }

    }

    @Test
    fun `nominal case`() = testCoroutineRule.runBlockingTest {
        // Given
        val mainActivityViewModel = getMainActivityViewModel()

        // When
        mainActivityViewModel.onWantToSeeMyRestaurant()
        val result = mainActivityViewModel.selectedRestaurantIdLiveData.getOrAwaitValue()
        val expected = SelectedRestaurant(id = PLACE_ID)
        // Then
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `no restaurant selected case because no corresponding email`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val mainActivityViewModel = getMainActivityViewModel()
            // Mock firestore repository
            coEvery {
                firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
            } returns flowOf(
                listOf(
                    WorkmateWithSelectedRestaurant(
                        workmateName = OTHER_WORKMATE_NAME,
                        workmateEmail = OTHER_WORKMATE_EMAIL,
                        workmatePhotoUrl = OTHER_WORKMATE_PHOTO_URL,
                        selectedRestaurantName = PLACE_NAME,
                        selectedRestaurantId = PLACE_ID
                    )
                )
            )

            // When
            mainActivityViewModel.onWantToSeeMyRestaurant()
            val result = mainActivityViewModel.selectedRestaurantIdLiveData.getOrAwaitValue()
            val expected = NoRestaurantSelected
            // Then
            Assert.assertEquals(expected, result)
        }

    @Test
    fun `no restaurant selected case because no restaurant selected by user`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val mainActivityViewModel = getMainActivityViewModel()
            // Mock firestore repository
            coEvery {
                firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
            } returns flowOf(
                listOf()
            )

            // When
            mainActivityViewModel.onWantToSeeMyRestaurant()
            val result = mainActivityViewModel.selectedRestaurantIdLiveData.getOrAwaitValue()
            val expected = NoRestaurantSelected
            // Then
            Assert.assertEquals(expected, result)
        }

    @Test
    fun `no search string send if length less than 3 car`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val mainActivityViewModel = getMainActivityViewModel()

            // When
            mainActivityViewModel.onSearchTextChange(newSearchText = LESS_THAN_3_CHAR)

            // Then
            verify { searchUseCaseMock.updateSearchText(SearchUseCase.SearchStringStatus.EmptyString) }
        }

    @Test
    fun `no search string send if null`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val mainActivityViewModel = getMainActivityViewModel()

            // When
            mainActivityViewModel.onSearchTextChange(newSearchText = nullSearchString)

            // Then
            verify { searchUseCaseMock.updateSearchText(SearchUseCase.SearchStringStatus.EmptyString) }
        }

    @Test
    fun `search string send if length more than 3 car`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val mainActivityViewModel = getMainActivityViewModel()

            // When
            mainActivityViewModel.onSearchTextChange(newSearchText = MORE_THAN_3_CHAR)

            // Then
            verify { searchUseCaseMock.updateSearchText(SearchUseCase.SearchStringStatus.SearchString(data = MORE_THAN_3_CHAR)) }
        }



    // region IN

    private fun getMainActivityViewModel() =
        MainActivityViewModel(
            coroutinesProvider = coroutinesProviderMock,
            firebaseAuthUtils = firebaseAuthUtilsMockk,
            firestoreRepository = firestoreRepositoryMockk,
            searchUseCase = searchUseCaseMock
        )

    // endregion
}