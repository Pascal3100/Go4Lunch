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
import fr.plopez.go4lunch.tests.utils.CommonsUtils
import fr.plopez.go4lunch.tests.utils.CommonsUtils.ADDRESS
import fr.plopez.go4lunch.tests.utils.CommonsUtils.GOOGLE_PHOTOS_API_URL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LATITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LONGITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PHOTO_MAX_WIDTH
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PHOTO_URL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.QUERY_TIME_STAMP
import fr.plopez.go4lunch.tests.utils.CommonsUtils.RATE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultPhotoUrl
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultRestaurantEntityList
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultRestaurantOpeningPeriodList
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.main_activity.list_restaurants.ListRestaurantsViewModel
import fr.plopez.go4lunch.view.model.RestaurantItemViewState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ListRestaurantViewModelTest {

    companion object {
        private const val DEFAULT_OPENING_STATE_TEXT = "open until 12:00"
        private const val DEFAULT_DISTANCE_TO_USER = "100m"

    }

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()
    private val restaurantsRepositoryMockK = mockk<RestaurantsRepository>()
    private val contextMockK = mockk<Context>()

    // Test variables
    private lateinit var listRestaurantsViewModel: ListRestaurantsViewModel

    @Before
    fun setUp() {
        // Coroutines provider mock - provides a specific dispatcher for tests
        every { coroutinesProviderMock.ioCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher

        // Mock restaurant repository
        coEvery { restaurantsRepositoryMockK.lastRequestTimeStampSharedFlow } returns
                flowOf(QUERY_TIME_STAMP) as SharedFlow<Long>

        coEvery { restaurantsRepositoryMockK.getRestaurantsWithOpeningPeriods(QUERY_TIME_STAMP) } returns
                getDefaultRestaurantWithOpeningPeriodsList()

        coEvery { restaurantsRepositoryMockK.getPositionForTimestamp(QUERY_TIME_STAMP) } returns
                getDefaultRestaurantsQuery()

        every { contextMockK.resources.getString(R.string.place_photo_api_url) } returns
                "https://maps.googleapis.com/maps/api/place/photo?"
    }

    @Test
    fun `nominal case`() = testCoroutineRule.runBlockingTest {
        // Given

        // When
        listRestaurantsViewModel.restaurantsItemsLiveData.observeForever {
            // Then
            assertEquals(getRestaurantItemViewStateList(), it)
        }
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

    private fun getRestaurantItemViewStateList(
        openingStateText: String = getDefaultOpeningStateText(),
        distanceToUser: String = getDefaultDistanceToUser(),
        workmates: String = getDefaultWorkmates(),
        photoUrl: String = getDefaultPhotoUrl()
    ) = listOf(
        RestaurantItemViewState(
            NAME,
            ADDRESS,
            openingStateText,
            distanceToUser,
            workmates,
            RATE.toFloat(),
            photoUrl,
            PLACE_ID
        )
    )

    private fun getDefaultOpeningStateText() = DEFAULT_OPENING_STATE_TEXT
    private fun getDefaultDistanceToUser() = DEFAULT_DISTANCE_TO_USER
    // TODO NOT FINISHED
    private fun getDefaultWorkmates() = "0"

    // endregion

}
