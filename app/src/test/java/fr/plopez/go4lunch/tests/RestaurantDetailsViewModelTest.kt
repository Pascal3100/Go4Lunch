package fr.plopez.go4lunch.tests

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.User
import fr.plopez.go4lunch.data.Workmate
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.data.di.CoroutinesProvider
import fr.plopez.go4lunch.data.di.BuildConfigProvider
import fr.plopez.go4lunch.tests.utils.CommonsUtils
import fr.plopez.go4lunch.tests.utils.CommonsUtils.MAX_WIDTH
import fr.plopez.go4lunch.tests.utils.CommonsUtils.NEARBY_KEY
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ADDRESS
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_PHONE_NUMBER
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_PHOTO_API_URL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_PHOTO_URL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_RATE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_WEBSITE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_EMAIL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_PHOTO_URL
import fr.plopez.go4lunch.tests.utils.LiveDataUtils.getOrAwaitValue
import fr.plopez.go4lunch.utils.DateTimeUtils
import fr.plopez.go4lunch.utils.FirebaseAuthUtils
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewState
import fr.plopez.go4lunch.view.model.WorkmateViewState
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewModel
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewModel.RestaurantDetailsViewAction.FirestoreFails
import fr.plopez.go4lunch.view.restaurant_details.RestaurantDetailsViewModel.RestaurantDetailsViewAction.FirestoreWorks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RestaurantDetailsViewModelTest {

    companion object {
        private const val WORKMATE_HAS_JOINED = "$WORKMATE_NAME has joined!"
        private const val WORKMATE_HAS_JOINED_STYLE = R.style.workmateItemNormalBlackBoldTextAppearance
        private const val OTHER_PLACE_ID = "OTHER_PLACE_ID"
        private const val OTHER_PLACE_NAME = "OTHER_PLACE_NAME"
        private const val DELAY = 666L
    }

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()
    private val restaurantsRepositoryMockK = mockk<RestaurantsRepository>()
    private val firestoreRepositoryMockk = mockk<FirestoreRepository>()
    private val firebaseAuthUtilsMockk = mockk<FirebaseAuthUtils>()
    private val contextMockK = mockk<Context>()
    private val nearbyConstantsMockK = mockk<BuildConfigProvider>()
    private val stateMockk = mockk<SavedStateHandle>(relaxed = true)
    private val dateTimeUtilsMockk = mockk<DateTimeUtils>()

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
            firestoreRepositoryMockk.getLikedRestaurants()
        } returns flowOf(listOf(PLACE_ID))

        coEvery {
            firestoreRepositoryMockk.addOrSuppressLikedRestaurant(
                placeId = PLACE_ID,
                likeState = false
            )
        } returns true

        coEvery {
            firestoreRepositoryMockk.addOrSuppressLikedRestaurant(
                placeId = PLACE_ID,
                likeState = true
            )
        } returns true

        coEvery {
            firestoreRepositoryMockk.setOrUnsetSelectedRestaurant(
                placeId = PLACE_ID,
                selectedState = false
            )
        } returns true

        coEvery {
            firestoreRepositoryMockk.setOrUnsetSelectedRestaurant(
                placeId = PLACE_ID,
                selectedState = true
            )
        } returns true

        // Restaurant repository Mock
        coEvery {
            restaurantsRepositoryMockK.getRestaurantFromId(PLACE_ID)
        } returns CommonsUtils.getDefaultRestaurantEntityList().first()

        // Firebase Auth Mock
        coEvery { firebaseAuthUtilsMockk.getUser() } returns User(
            name = WORKMATE_NAME,
            email = WORKMATE_EMAIL,
            photoUrl = WORKMATE_PHOTO_URL
        )
        coEvery { firebaseAuthUtilsMockk.getWorkmate() } returns Workmate(
            name = WORKMATE_NAME,
            email = WORKMATE_EMAIL,
            photoUrl = WORKMATE_PHOTO_URL
        )

        // Context Mock
        every {
            contextMockK.resources.getString(R.string.joined_workmate_message, WORKMATE_NAME)
        } returns WORKMATE_HAS_JOINED

        every {
            contextMockK.resources.getString(R.string.place_photo_api_url, MAX_WIDTH, PLACE_PHOTO_URL, NEARBY_KEY)
        } returns PLACE_PHOTO_API_URL

        // SavedStateHandle Mockk
        every {
            stateMockk.getLiveData<String>(PLACE_ID)
        } returns MutableLiveData(PLACE_ID)

        // Nearby Constants Mockk
        every {
            nearbyConstantsMockK.key
        } returns NEARBY_KEY

        // DateTimeUtils Mockk
        every { dateTimeUtilsMockk.getDelayUtilLunch() } returns DELAY
    }

    @Test
    fun `nominal case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()

        // When
        val result = restaurantDetailsViewModel.restaurantDetailsViewStateLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantDetailsViewState(), result)
    }

    @Test
    fun `restaurant not liked because not this restaurant id in list case`() =
        testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()

        coEvery {
            firestoreRepositoryMockk.getLikedRestaurants()
        } returns flowOf(listOf(OTHER_PLACE_ID))


        // When
        val result = restaurantDetailsViewModel.restaurantDetailsViewStateLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantDetailsViewState(
            isFavorite = false
        ), result)
    }

    @Test
    fun `restaurant not liked because no restaurant id in list case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()

        coEvery {
            firestoreRepositoryMockk.getLikedRestaurants()
        } returns flowOf(emptyList())


        // When
        val result = restaurantDetailsViewModel.restaurantDetailsViewStateLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantDetailsViewState(
            isFavorite = false
        ), result)
    }

    @Test
    fun `restaurant not selected because not this restaurant id selected by user case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()

        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(
            listOf(
                WorkmateWithSelectedRestaurant(
                    workmateName = WORKMATE_NAME,
                    workmateEmail = WORKMATE_EMAIL,
                    workmatePhotoUrl = WORKMATE_PHOTO_URL,
                    selectedRestaurantName = OTHER_PLACE_NAME,
                    selectedRestaurantId = OTHER_PLACE_ID
                )
            )
        )

        // When
        val result = restaurantDetailsViewModel.restaurantDetailsViewStateLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantDetailsViewState(
            isSelected = false,
            interestedWorkmatesList = emptyList()
        ), result)
    }

    @Test
    fun `restaurant not selected because no restaurant selected by user case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()

        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(
            emptyList()
        )

        // When
        val result = restaurantDetailsViewModel.restaurantDetailsViewStateLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantDetailsViewState(
            isSelected = false,
            interestedWorkmatesList = emptyList()
        ), result)
    }

    @Test
    fun `restaurant not selected and not liked case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()

        coEvery {
            firestoreRepositoryMockk.getLikedRestaurants()
        } returns flowOf(emptyList())

        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(
            emptyList()
        )

        // When
        val result = restaurantDetailsViewModel.restaurantDetailsViewStateLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantDetailsViewState(
            isFavorite = false,
            isSelected = false,
            interestedWorkmatesList = emptyList()
        ), result)
    }

    @Test
    fun `restaurant is liked and firestore doesn't fail case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()

        // When
        restaurantDetailsViewModel.onLike()
        val firestoreStateResult = restaurantDetailsViewModel.firestoreStateLiveData.getOrAwaitValue()

        // Then
        // check view action update
        assertEquals(FirestoreWorks, firestoreStateResult)
    }

    @Test
    fun `restaurant is liked and firestore fail case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()
        coEvery {
            firestoreRepositoryMockk.addOrSuppressLikedRestaurant(
                placeId = PLACE_ID,
                likeState = false
            )
        } returns false

        // When
        restaurantDetailsViewModel.onLike()
        val firestoreStateResult = restaurantDetailsViewModel.firestoreStateLiveData.getOrAwaitValue()

        // Then
        // check view action update
        assertEquals(FirestoreFails, firestoreStateResult)
    }

    @Test
    fun `restaurant is selected and firestore doesn't fail case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()

        // When
        restaurantDetailsViewModel.onSelectRestaurant()
        val firestoreStateResult = restaurantDetailsViewModel.firestoreStateLiveData.getOrAwaitValue()

        // Then
        // check view action update
        assertEquals(FirestoreWorks, firestoreStateResult)
    }

    @Test
    fun `restaurant is selected and firestore fail case`() = testCoroutineRule.runBlockingTest {
        // Given
        val restaurantDetailsViewModel = getRestaurantDetailsViewModel()
        coEvery {
            firestoreRepositoryMockk.setOrUnsetSelectedRestaurant(
                placeId = PLACE_ID,
                selectedState = false
            )
        } returns false

        // When
        restaurantDetailsViewModel.onSelectRestaurant()
        val firestoreStateResult = restaurantDetailsViewModel.firestoreStateLiveData.getOrAwaitValue()

        // Then
        // check view action update
        assertEquals(FirestoreFails, firestoreStateResult)
    }

    private fun getDefaultRestaurantDetailsViewState(
        isSelected: Boolean = true,
        isFavorite: Boolean = true,
        interestedWorkmatesList: List<WorkmateViewState> = getDefaultWorkmatesViewStateList()
    ) = RestaurantDetailsViewState(
        photoUrl = PLACE_PHOTO_API_URL,
        name = PLACE_NAME,
        id = PLACE_ID,
        address = PLACE_ADDRESS,
        rate = PLACE_RATE.toFloat(),
        phoneNumber = PLACE_PHONE_NUMBER,
        website = PLACE_WEBSITE,
        isSelected = isSelected,
        isFavorite = isFavorite,
        interestedWorkmatesList = interestedWorkmatesList,
        currentUserEmail = WORKMATE_EMAIL,
        delay = DELAY
    )

    private fun getDefaultWorkmatesViewStateList() =
        listOf(
            WorkmateViewState(
                text = WORKMATE_HAS_JOINED,
                photoUrl = WORKMATE_PHOTO_URL,
                style = WORKMATE_HAS_JOINED_STYLE
            )
        )

    private fun getRestaurantDetailsViewModel() = RestaurantDetailsViewModel(
        restaurantsRepository = restaurantsRepositoryMockK,
        firestoreRepository = firestoreRepositoryMockk,
        firebaseAuthUtils = firebaseAuthUtilsMockk,
        coroutinesProvider = coroutinesProviderMock,
        nearbyConstants = nearbyConstantsMockK,
        state = stateMockk,
        context = contextMockK,
        dateTimeUtils = dateTimeUtilsMockk
    )
}