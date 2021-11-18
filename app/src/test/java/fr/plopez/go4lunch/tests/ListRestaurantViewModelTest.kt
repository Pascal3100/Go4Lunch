package fr.plopez.go4lunch.tests

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantOpeningPeriod
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.RestaurantWithOpeningPeriods
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.di.NearbyConstants
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ADDRESS
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LATITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LONGITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_PHOTO_URL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.QUERY_TIME_STAMP
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_RATE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultRestaurantEntityList
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultRestaurantOpeningPeriodList
import fr.plopez.go4lunch.tests.utils.LiveDataUtils.getOrAwaitValue
import fr.plopez.go4lunch.utils.DateTimeUtils
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.main_activity.list_restaurants.ListRestaurantsViewModel
import fr.plopez.go4lunch.view.model.RestaurantItemViewState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

@ExperimentalCoroutinesApi
class ListRestaurantViewModelTest {

    companion object {
        private const val DEFAULT_OPENING_STATE_TEXT = "open until 14:00"

        // No matter what, distance to user will always be 0 in unit tests because using Location.distanceTo
        // more info at https://stackoverflow.com/questions/45660282/call-to-android-api-always-returns-0-in-a-unit-test/45665776
        private const val DEFAULT_DISTANCE_TO_USER = "0 m"

        // Here day is in Calendar Format so Sunday = 1 -> Saturday = 7
        // correspond to Wednesday
        private const val DEFAULT_DAY = 4
        // correspond to Monday
        private const val CLOSED_DAY = 2
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

    // Test variables
    private val nearbyConstants = NearbyConstants()

    private val defaultOpeningHour = LocalTime.of(11,30)
    private val firstClosedHour = LocalTime.of(9,30)
    private val intermediateClosedHour = LocalTime.of(16,30)
    private val lastClosedHour = LocalTime.of(23,30)

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

        every { contextMockK.resources.getString(R.string.place_photo_api_url) } returns
                "https://maps.googleapis.com/maps/api/place/photo?"
        every { contextMockK.resources.getString(R.string.no_available_hours) } returns
                "no opening hours available"
        every { contextMockK.resources.getString(R.string.closed_today_text) } returns
                "closed today"
        every { contextMockK.resources.getString(R.string.closed_text) } returns
                "closed"
        every { contextMockK.resources.getString(R.string.open_at_text) } returns
                "open at "
        every { contextMockK.resources.getString(R.string.open_until_text) } returns
                "open until "
        every { contextMockK.resources.getString(R.string.distance_unit) } returns
                " m"
    }

    @Test
    fun `nominal case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = ListRestaurantsViewModel(
            restaurantsRepository = restaurantsRepositoryMockK,
            nearbyConstants = nearbyConstants,
            coroutinesProvider = coroutinesProviderMock,
            dateTimeUtils = dateTimeUtilsMockk,
            context = contextMockK
        )

        // When
        listRestaurantsViewModel.restaurantsItemsLiveData.observeForever {
            // Then
            assertEquals(getDefaultRestaurantItemViewStateList(), it)
        }
    }

    @Test
    fun `current day not in openings days case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = ListRestaurantsViewModel(
            restaurantsRepository = restaurantsRepositoryMockK,
            nearbyConstants = nearbyConstants,
            coroutinesProvider = coroutinesProviderMock,
            dateTimeUtils = dateTimeUtilsMockk,
            context = contextMockK
        )
        every { dateTimeUtilsMockk.getCurrentDay() } returns CLOSED_DAY

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantItemViewStateList(openingStateText = "closed today"), result)
    }

    @Test
    fun `current hour is before first opening hour case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = ListRestaurantsViewModel(
            restaurantsRepository = restaurantsRepositoryMockK,
            nearbyConstants = nearbyConstants,
            coroutinesProvider = coroutinesProviderMock,
            dateTimeUtils = dateTimeUtilsMockk,
            context = contextMockK
        )
        every { dateTimeUtilsMockk.getCurrentTime() } returns firstClosedHour

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantItemViewStateList(openingStateText = "open at 11:00"), result)
    }

    @Test
    fun `current hour is between first and second opening period case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = ListRestaurantsViewModel(
            restaurantsRepository = restaurantsRepositoryMockK,
            nearbyConstants = nearbyConstants,
            coroutinesProvider = coroutinesProviderMock,
            dateTimeUtils = dateTimeUtilsMockk,
            context = contextMockK
        )
        every { dateTimeUtilsMockk.getCurrentTime() } returns intermediateClosedHour

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantItemViewStateList(openingStateText = "open at 18:00"), result)
    }

    @Test
    fun `current hour is after last closing hour case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = ListRestaurantsViewModel(
            restaurantsRepository = restaurantsRepositoryMockK,
            nearbyConstants = nearbyConstants,
            coroutinesProvider = coroutinesProviderMock,
            dateTimeUtils = dateTimeUtilsMockk,
            context = contextMockK
        )
        every { dateTimeUtilsMockk.getCurrentTime() } returns lastClosedHour

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantItemViewStateList(openingStateText = "closed"), result)
    }

    @Test
    fun `no periods available case`() = testCoroutineRule.runBlockingTest {
        // Given
        val listRestaurantsViewModel = ListRestaurantsViewModel(
            restaurantsRepository = restaurantsRepositoryMockK,
            nearbyConstants = nearbyConstants,
            coroutinesProvider = coroutinesProviderMock,
            dateTimeUtils = dateTimeUtilsMockk,
            context = contextMockK
        )
        coEvery { restaurantsRepositoryMockK.getRestaurantsWithOpeningPeriods(QUERY_TIME_STAMP) } returns
                getDefaultRestaurantWithOpeningPeriodsList(restaurantOpeningPeriodList = emptyList())

        // When
        val result = listRestaurantsViewModel.restaurantsItemsLiveData.getOrAwaitValue()

        // Then
        assertEquals(getDefaultRestaurantItemViewStateList(openingStateText = "no opening hours available"), result)
    }

    // region in

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
        openingStateText: String = getDefaultOpeningStateText(),
        distanceToUser: String = getDefaultDistanceToUser(),
        workmates: String = getDefaultWorkmates(),
        photoUrl: String = PLACE_PHOTO_URL
    ) = listOf(
        RestaurantItemViewState(
            name = PLACE_NAME,
            address = PLACE_ADDRESS,
            openingStateText = openingStateText,
            distanceToUser = distanceToUser,
            numberOfInterestedWorkmates = workmates,
            rate = PLACE_RATE.toFloat(),
            photoUrl = photoUrl,
            id = PLACE_ID
        )
    )

    private fun getDefaultOpeningStateText() = DEFAULT_OPENING_STATE_TEXT
    private fun getDefaultDistanceToUser() = DEFAULT_DISTANCE_TO_USER
    private fun getDefaultWorkmates() = "0"

    // endregion

}
