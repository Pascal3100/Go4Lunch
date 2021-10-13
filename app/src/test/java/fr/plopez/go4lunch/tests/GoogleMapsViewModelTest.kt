package fr.plopez.go4lunch.tests

import android.content.Context
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.main_activity.google_maps.GoogleMapsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class GoogleMapsViewModelTest {
    companion object {
        private const val LATITUDE = "90.0"
        private const val LONGITUDE = "0.0"
        private const val ZOOM = "15.0"
        private const val RADIUS = "1000"

        private const val PLACE_ID = "PLACE_ID"
        private const val NAME = "NAME"
        private const val ADDRESS = "ADDRESS"
        private const val PHOTO_URL = "PHOTO_URL"
        private const val RATE = "5.0"
        private const val PHONE_NUMBER = "+33 0 00 00 00 00"
        private const val WEBSITE = "www.no-web-site.com"
    }

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    // Mocks
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()
    private val locationRepositoryMockK = mockk<LocationRepository>()
    private val restaurantsRepositoryMockK = mockk<RestaurantsRepository>()
    private val contextMockK = mockk<Context>()

    // Test variables

    private lateinit var googleMapsViewModel: GoogleMapsViewModel

    @Before
    fun setUp() {
        // Coroutines provider mock - provides a specific dispatcher for tests
        every { coroutinesProviderMock.ioCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher

        // Mock location repository
        coEvery { locationRepositoryMockK.fetchUpdates() } returns flowOf(
            LocationRepository.PositionWithZoom(
                LATITUDE.toDouble(),
                LONGITUDE.toDouble(),
                ZOOM.toFloat()
            )
        )

        // Mock restaurant repository
        coEvery {
            restaurantsRepositoryMockK.getRestaurantsAroundPosition(
                LATITUDE,
                LONGITUDE,
                RADIUS
            )
        } returns flowOf(
            RestaurantsRepository.ResponseStatus.Success(
                getExpectedRestaurantEntityList()
            )
        )

        // Mock Context
        every {
            contextMockK.resources.getString(R.string.default_detection_radius_value)
        } returns RADIUS

        googleMapsViewModel = GoogleMapsViewModel(
            locationRepository = locationRepositoryMockK,
            restaurantsRepository = restaurantsRepositoryMockK,
            coroutinesProvider = coroutinesProviderMock,
            context = contextMockK
        )
    }

    @Test
    fun `success response when a list of restaurant is returned`() =
        testCoroutineRule.runBlockingTest {
            // Given

            // When
            googleMapsViewModel.onMapReady()
            val result = googleMapsViewModel.googleMapViewStateSharedFlow.first()

            // Then
            val expectedResult = getExpectedGoogleMapViewState()
            assertEquals(expectedResult, result)
        }

    @Test
    fun `no restaurant message when no response is returned`() =
        testCoroutineRule.runBlockingTest {
            // Given
            coEvery {
                restaurantsRepositoryMockK.getRestaurantsAroundPosition(
                    LATITUDE,
                    LONGITUDE,
                    RADIUS
                )
            } returns flowOf(
                RestaurantsRepository.ResponseStatus.NoResponse
            )

            // When
            googleMapsViewModel.onMapReady()
            val result = googleMapsViewModel.googleMapViewActionFlow.first()

            // Then
            val expectedResult = GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage(
                R.string.no_response_message
            )
            assertEquals(expectedResult, result)
        }

    @Test
    fun `http error message when http exception is returned`() =
        testCoroutineRule.runBlockingTest {
            // Given
            coEvery {
                restaurantsRepositoryMockK.getRestaurantsAroundPosition(
                    LATITUDE,
                    LONGITUDE,
                    RADIUS
                )
            } returns flowOf(
                RestaurantsRepository.ResponseStatus.StatusError.HttpException
            )

            // When
            googleMapsViewModel.onMapReady()
            val result = googleMapsViewModel.googleMapViewActionFlow.first()

            // Then
            val expectedResult = GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage(
                R.string.network_error_message
            )
            assertEquals(expectedResult, result)
        }

    @Test
    fun `internet error message when io exception is returned`() =
        testCoroutineRule.runBlockingTest {
            // Given
            coEvery {
                restaurantsRepositoryMockK.getRestaurantsAroundPosition(
                    LATITUDE,
                    LONGITUDE,
                    RADIUS
                )
            } returns flowOf(
                RestaurantsRepository.ResponseStatus.StatusError.IOException
            )

            // When
            googleMapsViewModel.onMapReady()
            val result = googleMapsViewModel.googleMapViewActionFlow.first()

            // Then
            val expectedResult = GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage(
                R.string.no_internet_message
            )
            assertEquals(expectedResult, result)
        }

    // region IN
    private fun getExpectedRestaurantEntityList() = listOf(
        RestaurantEntity(
            restaurantId = PLACE_ID,
            name = NAME,
            address = ADDRESS,
            latitude = LATITUDE.toDouble(),
            longitude = LONGITUDE.toDouble(),
            photoUrl = PHOTO_URL,
            rate = RATE.toFloat(),
            phoneNumber = PHONE_NUMBER,
            website = WEBSITE
        )
    )

    private fun getExpectedGoogleMapViewState() = GoogleMapsViewModel.GoogleMapViewState(
            LATITUDE.toDouble(),
            LONGITUDE.toDouble(),
            ZOOM.toFloat(),
            listOf(
                GoogleMapsViewModel.RestaurantViewState(
                    latitude = LATITUDE.toDouble(),
                    longitude = LONGITUDE.toDouble(),
                    name = NAME
                )
            ))

    // endregion
}