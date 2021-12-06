package fr.plopez.go4lunch.tests

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.di.BuildConfigProvider
import fr.plopez.go4lunch.tests.utils.CommonsUtils
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ADDRESS
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LATITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LONGITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.MAX_WIDTH
import fr.plopez.go4lunch.tests.utils.CommonsUtils.NEARBY_KEY
import fr.plopez.go4lunch.tests.utils.CommonsUtils.OTHER_PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.OTHER_PLACE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_PHOTO_API_URL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_PHOTO_URL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.QUERY_TIME_STAMP
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_RATE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultRestaurantEntityList
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultRestaurantOpeningPeriodList
import fr.plopez.go4lunch.tests.utils.LiveDataUtils.getOrAwaitValue
import fr.plopez.go4lunch.utils.DateTimeUtils
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.main_activity.SearchUseCase
import fr.plopez.go4lunch.view.main_activity.SearchUseCase.SearchResultStatus
import fr.plopez.go4lunch.view.main_activity.SearchUseCase.SearchResultStatus.EmptyQuery
import fr.plopez.go4lunch.view.main_activity.list_restaurants.ListRestaurantsViewModel
import fr.plopez.go4lunch.view.main_activity.list_restaurants.RestaurantItemViewState
import fr.plopez.go4lunch.view.main_activity.list_workmates.WorkmateWithSelectedRestaurant
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

@FlowPreview
@ExperimentalCoroutinesApi
class ListRestaurantViewModelTest {

    companion object {
        // No matter what, distance to user will always be 0 in unit tests because using Location.distanceTo
        // more info at https://stackoverflow.com/questions/45660282/call-to-android-api-always-returns-0-in-a-unit-test/45665776
        private const val DEFAULT_DISTANCE_TO_USER = "0"

        // Here day is in Calendar Format so Sunday = 1 -> Saturday = 7
        // correspond to Wednesday
        private const val DEFAULT_DAY = 4

        // correspond to Monday
        private const val CLOSED_DAY = 2

        private const val CLOSED_TODAY = "closed today"
        private const val NO_OPENING_HOURS = "no opening hours available"
        private const val CLOSED = "closed"
        private const val OPEN_AT = "open at "
        private const val OPEN_UNTIL = "open until "

        private const val ONE_WORKMATE_HAS_JOINED = "1"
        private const val NO_WORKMATE_HAS_JOINED = "0"

        private const val JOINED_DISTANCE_TO_USER = "$DEFAULT_DISTANCE_TO_USER m"

        private val FIRST_OPENING_HOUR = LocalTime.of(11, 0)
        private val SECOND_OPENING_HOUR = LocalTime.of(18, 0)
        private val FIRST_CLOSING_HOUR = LocalTime.of(14, 0)
        private val SECOND_CLOSING_HOUR = LocalTime.of(22, 30)
        private val JOINED_FIRST_OPENING_HOUR = OPEN_AT + FIRST_OPENING_HOUR
        private val JOINED_SECOND_OPENING_HOUR = OPEN_AT + SECOND_OPENING_HOUR
        private val JOINED_FIRST_CLOSING_HOUR = OPEN_UNTIL + FIRST_CLOSING_HOUR
        private val JOINED_SECOND_CLOSING_HOUR = OPEN_UNTIL + SECOND_CLOSING_HOUR
    }

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()
    private val restaurantsRepositoryMockK = mockk<RestaurantsRepository>()
    private val dateTimeUtilsMockk = mockk<DateTimeUtils>()
    private val contextMockK = mockk<Context>()
    private val nearbyConstantsMockK = mockk<BuildConfigProvider>()
    private val firestoreRepositoryMockk = mockk<FirestoreRepository>()
    private val searchUseCaseMock = mockk<SearchUseCase>()


    // Test variables
    private val defaultOpeningHour = LocalTime.of(11, 30)
    private val firstClosedHour = LocalTime.of(9, 30)
    private val intermediateClosedHour = LocalTime.of(16, 30)
    private val lastClosedHour = LocalTime.of(23, 30)

    @Before
    fun setUp() {
        // Coroutines provider mock - provides a specific dispatcher for tests
        every { coroutinesProviderMock.ioCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher

        // Mock restaurant repository
        coEvery { restaurantsRepositoryMockK.lastRequestTimeStampSharedFlow } returns
                flowOf(QUERY_TIME_STAMP)

        coEvery { restaurantsRepositoryMockK.getRestaurantsWithOpeningPeriods(QUERY_TIME_STAMP) } returns
                getDefaultRestaurantWithOpeningPeriodsList()

        coEvery { restaurantsRepositoryMockK.getPositionForTimestamp(QUERY_TIME_STAMP) } returns
                getDefaultRestaurantsQuery()

        every { dateTimeUtilsMockk.getCurrentDay() } returns DEFAULT_DAY

        every { dateTimeUtilsMockk.getCurrentTime() } returns defaultOpeningHour

        every {
            contextMockK.resources.getString(
                R.string.place_photo_api_url,
                MAX_WIDTH, PLACE_PHOTO_URL, NEARBY_KEY
            )
        } returns PLACE_PHOTO_API_URL

        every { contextMockK.resources.getString(R.string.no_available_hours) } returns NO_OPENING_HOURS
        every { contextMockK.resources.getString(R.string.closed_today_text) } returns CLOSED_TODAY
        every { contextMockK.resources.getString(R.string.closed_text) } returns CLOSED

        every {
            contextMockK.resources.getString(
                R.string.open_at_text,
                FIRST_OPENING_HOUR
            )
        } returns JOINED_FIRST_OPENING_HOUR
        every {
            contextMockK.resources.getString(
                R.string.open_at_text,
                SECOND_OPENING_HOUR
            )
        } returns JOINED_SECOND_OPENING_HOUR
        every {
            contextMockK.resources.getString(
                R.string.open_until_text,
                FIRST_CLOSING_HOUR
            )
        } returns JOINED_FIRST_CLOSING_HOUR
        every {
            contextMockK.resources.getString(
                R.string.open_until_text,
                SECOND_CLOSING_HOUR
            )
        } returns JOINED_SECOND_CLOSING_HOUR

        every {
            contextMockK.resources.getString(
                R.string.distance_unit,
                DEFAULT_DISTANCE_TO_USER
            )
        } returns JOINED_DISTANCE_TO_USER

        // Nearby Constants Mockk
        every { nearbyConstantsMockK.key } returns NEARBY_KEY

        // Mock firestore repository
        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(
            listOf(
                WorkmateWithSelectedRestaurant(
                    workmateName = CommonsUtils.WORKMATE_NAME,
                    workmateEmail = CommonsUtils.WORKMATE_EMAIL,
                    workmatePhotoUrl = CommonsUtils.WORKMATE_PHOTO_URL,
                    selectedRestaurantName = PLACE_NAME,
                    selectedRestaurantId = PLACE_ID
                )
            )
        )

        // Mock searchUseCase
        justRun { searchUseCaseMock.updateSearchText(any()) }
        justRun { searchUseCaseMock.updateWorkmatesViewDisplayState(any()) }
        coEvery { searchUseCaseMock.getSearchResult() } returns
                getDefaultSearchResultFlow()
    }

    @Test
    fun `nominal case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = getListRestaurantsViewModel()

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantItemViewStateList(), result)
    }

    @Test
    fun `filtered result case no result found`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val listRestaurantsViewModel = getListRestaurantsViewModel()
            coEvery {
                firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
            } returns flowOf(
                listOf(
                    WorkmateWithSelectedRestaurant(
                        workmateName = CommonsUtils.WORKMATE_NAME,
                        workmateEmail = CommonsUtils.WORKMATE_EMAIL,
                        workmatePhotoUrl = CommonsUtils.WORKMATE_PHOTO_URL,
                        selectedRestaurantName = PLACE_NAME,
                        selectedRestaurantId = PLACE_ID
                    ),
                    WorkmateWithSelectedRestaurant(
                        workmateName = CommonsUtils.WORKMATE_NAME,
                        workmateEmail = CommonsUtils.WORKMATE_EMAIL,
                        workmatePhotoUrl = CommonsUtils.WORKMATE_PHOTO_URL,
                        selectedRestaurantName = OTHER_PLACE_NAME,
                        selectedRestaurantId = OTHER_PLACE_ID
                    )
                )
            )
            coEvery { searchUseCaseMock.getSearchResult() } returns
                    getDefaultSearchResultFlow(searchResult = SearchResultStatus.SearchResult(listOf()))


            // When
            val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

            // Then
            assertEquals(listOf<RestaurantItemViewState>(), result)
        }

    @Test
    fun `filtered result case one result found`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = getListRestaurantsViewModel()
        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(
            listOf(
                WorkmateWithSelectedRestaurant(
                    workmateName = CommonsUtils.WORKMATE_NAME,
                    workmateEmail = CommonsUtils.WORKMATE_EMAIL,
                    workmatePhotoUrl = CommonsUtils.WORKMATE_PHOTO_URL,
                    selectedRestaurantName = PLACE_NAME,
                    selectedRestaurantId = PLACE_ID
                ),
                WorkmateWithSelectedRestaurant(
                    workmateName = CommonsUtils.WORKMATE_NAME,
                    workmateEmail = CommonsUtils.WORKMATE_EMAIL,
                    workmatePhotoUrl = CommonsUtils.WORKMATE_PHOTO_URL,
                    selectedRestaurantName = OTHER_PLACE_NAME,
                    selectedRestaurantId = OTHER_PLACE_ID
                )
            )
        )
        coEvery { searchUseCaseMock.getSearchResult() } returns
                getDefaultSearchResultFlow(
                    searchResult = SearchResultStatus.SearchResult(
                        listOf(PLACE_ID)
                    )
                )


        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantItemViewStateList(), result)
    }

    @Test
    fun `no workmate has joined case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = getListRestaurantsViewModel()

        coEvery {
            firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
        } returns flowOf(emptyList())

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(
            getDefaultRestaurantItemViewStateList(
                numberOfInterestedWorkmates = NO_WORKMATE_HAS_JOINED
            ), result
        )
    }

    @Test
    fun `current day not in openings days case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = getListRestaurantsViewModel()
        every { dateTimeUtilsMockk.getCurrentDay() } returns CLOSED_DAY

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(
            getDefaultRestaurantItemViewStateList(openingStateText = "closed today"),
            result
        )
    }

    @Test
    fun `current hour is before first opening hour case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = getListRestaurantsViewModel()
        every { dateTimeUtilsMockk.getCurrentTime() } returns firstClosedHour

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(
            getDefaultRestaurantItemViewStateList(openingStateText = "open at 11:00"),
            result
        )
    }

    @Test
    fun `current hour is between first and second opening period case`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val listRestaurantsViewModel = getListRestaurantsViewModel()

            every { dateTimeUtilsMockk.getCurrentTime() } returns intermediateClosedHour

            // When
            val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

            // Then
            assertEquals(
                getDefaultRestaurantItemViewStateList(openingStateText = "open at 18:00"),
                result
            )
        }

    @Test
    fun `current hour is after last closing hour case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = getListRestaurantsViewModel()

        every { dateTimeUtilsMockk.getCurrentTime() } returns lastClosedHour

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantItemViewStateList(openingStateText = "closed"), result)
    }

    @Test
    fun `no periods available case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = getListRestaurantsViewModel()

        coEvery { restaurantsRepositoryMockK.getRestaurantsWithOpeningPeriods(QUERY_TIME_STAMP) } returns
                getDefaultRestaurantWithOpeningPeriodsList(restaurantOpeningPeriodList = emptyList())

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(
            getDefaultRestaurantItemViewStateList(openingStateText = "no opening hours available"),
            result
        )
    }

    // region in

    private fun getListRestaurantsViewModel() = ListRestaurantsViewModel(
        restaurantsRepository = restaurantsRepositoryMockK,
        nearbyConstants = nearbyConstantsMockK,
        coroutinesProvider = coroutinesProviderMock,
        dateTimeUtils = dateTimeUtilsMockk,
        context = contextMockK,
        firestoreRepository = firestoreRepositoryMockk,
        searchUseCase = searchUseCaseMock
    )

    private fun getDefaultRestaurantWithOpeningPeriodsList(
        restaurantEntity: RestaurantEntity = getDefaultRestaurantEntityList().first(),
        restaurantOpeningPeriodList: List<RestaurantOpeningPeriod> = getDefaultRestaurantOpeningPeriodList()
    ): List<RestaurantWithOpeningPeriods> =
        listOf(
            RestaurantWithOpeningPeriods(
                restaurant = restaurantEntity,
                openingHours = restaurantOpeningPeriodList
            )
        )

    private fun getDefaultRestaurantsQuery() =
        RestaurantsQuery(
            queryTimeStamp = QUERY_TIME_STAMP,
            latitude = LATITUDE.toDouble(),
            longitude = LONGITUDE.toDouble()
        )

    private fun getDefaultRestaurantItemViewStateList(
        openingStateText: String = JOINED_FIRST_CLOSING_HOUR,
        distanceToUser: String = JOINED_DISTANCE_TO_USER,
        numberOfInterestedWorkmates: String = ONE_WORKMATE_HAS_JOINED,
        photoUrl: String = PLACE_PHOTO_API_URL
    ) = listOf(
        RestaurantItemViewState(
            name = PLACE_NAME,
            address = PLACE_ADDRESS,
            openingStateText = openingStateText,
            distanceToUser = distanceToUser,
            numberOfInterestedWorkmates = numberOfInterestedWorkmates,
            rate = PLACE_RATE.toFloat(),
            photoUrl = photoUrl,
            id = PLACE_ID
        )
    )

    private fun getDefaultSearchResultFlow(
        searchResult: SearchResultStatus = EmptyQuery
    ) = flowOf(searchResult)

    // endregion

}
