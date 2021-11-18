package fr.plopez.go4lunch.tests

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.Workmate
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.tests.utils.CommonsUtils
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_EMAIL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_PHOTO_URL
import fr.plopez.go4lunch.tests.utils.LiveDataUtils.getOrAwaitValue
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.main_activity.list_workmates.ListWorkmatesViewModel
import fr.plopez.go4lunch.view.model.WorkmateViewState
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ListWorkmatesViewModelTest {

    companion object {
        private const val WORKMATE_HAS_DECIDED = "WORKMATE_NAME is eating at PLACE_NAME"
        private const val WORKMATE_HAS_NOT_DECIDED = "WORKMATE_NAME has not decided yet."

        private const val WORKMATE_HAS_NOT_DECIDED_STYLE =
            R.style.workmateItemNormalGhostGreyItalicTextAppearance
        private const val WORKMATE_HAS_DECIDED_STYLE =
            R.style.workmateItemNormalBlackBoldTextAppearance
    }

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()
    private val firestoreRepositoryMockk = mockk<FirestoreRepository>()
    private val contextMockK = mockk<Context>()

    // Test variables

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
        coEvery {
            firestoreRepositoryMockk.getWorkmatesUpdates()
        } returns flowOf(
            getDefaultWorkmatesList()
        )

        // Mock Context
        every {
            contextMockK.resources.getString(
                R.string.workmate_has_not_decided,
                WORKMATE_NAME
            )
        } returns WORKMATE_HAS_NOT_DECIDED
        every {
            contextMockK.resources.getString(
                R.string.workmate_has_decided,
                WORKMATE_NAME,
                PLACE_NAME
            )
        } returns WORKMATE_HAS_DECIDED

    }

    private fun getDefaultWorkmatesList(
        name: String = WORKMATE_NAME,
    ) = listOf(
        Workmate(
            name = WORKMATE_NAME,
            email = WORKMATE_EMAIL,
            photoUrl = WORKMATE_PHOTO_URL
        )
    )

    // Nominal case
    @Test
    fun `nominal case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listWorkmatesViewModel = ListWorkmatesViewModel(
            firestoreRepository = firestoreRepositoryMockk,
            coroutinesProvider = coroutinesProviderMock,
            context = contextMockK
        )

        // When
        val result = listWorkmatesViewModel.getWorkmatesUpdates().getOrAwaitValue()

        // Then
        assertEquals(getDefaultWorkmateViewState(), result)
    }

    @Test
    fun `no workmates case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listWorkmatesViewModel = ListWorkmatesViewModel(
            firestoreRepository = firestoreRepositoryMockk,
            coroutinesProvider = coroutinesProviderMock,
            context = contextMockK
        )
        // Mock firestore repository
        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(
            emptyList()
        )
        coEvery {
            firestoreRepositoryMockk.getWorkmatesUpdates()
        } returns flowOf(
            emptyList()
        )

        // When
        val result = listWorkmatesViewModel.getWorkmatesUpdates().getOrAwaitValue()

        // Then
        assertEquals(emptyList<WorkmateViewState>(), result)
    }

    @Test
    fun `no workmates has selected case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listWorkmatesViewModel = ListWorkmatesViewModel(
            firestoreRepository = firestoreRepositoryMockk,
            coroutinesProvider = coroutinesProviderMock,
            context = contextMockK
        )
        // Mock firestore repository
        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(
            emptyList()
        )
        coEvery {
            firestoreRepositoryMockk.getWorkmatesUpdates()
        } returns flowOf(
            getDefaultWorkmatesList()
        )

        // When
        val result = listWorkmatesViewModel.getWorkmatesUpdates().getOrAwaitValue()

        // Then
        assertEquals(getDefaultWorkmateViewState(
            text = WORKMATE_HAS_NOT_DECIDED,
            style = WORKMATE_HAS_NOT_DECIDED_STYLE
        ), result)
    }

    private fun getDefaultWorkmateViewState(
        text: String = WORKMATE_HAS_DECIDED,
        style: Int = WORKMATE_HAS_DECIDED_STYLE
    ) = listOf(
        WorkmateViewState(
            text = text,
            photoUrl = WORKMATE_PHOTO_URL,
            style = style
        )
    )
}