package fr.plopez.go4lunch.tests

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LATITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LONGITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.RADIUS
import fr.plopez.go4lunch.tests.utils.CommonsUtils.ZOOM
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultRestaurantEntityList
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.main_activity.google_maps.GoogleMapsViewModel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class GoogleMapsViewModelTest {

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
                getDefaultRestaurantEntityList()
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
            googleMapsViewModel.googleMapViewStateLiveData.observeForever {
                // Then
                val expectedResult = getDefaultGoogleMapViewState()
                assertEquals(expectedResult, it)
            }
        }

    // TODO @Nino this test not working
    @Test
    fun `NoRestaurant response when list of restaurant is empty`() =
        testCoroutineRule.runBlockingTest {
            // Given
            coEvery {
                restaurantsRepositoryMockK.getRestaurantsAroundPosition(
                    LATITUDE,
                    LONGITUDE,
                    RADIUS
                )
            } returns flowOf(
                RestaurantsRepository.ResponseStatus.NoRestaurants
            )

            // When
            googleMapsViewModel.onMapReady()

            // Verify that right camera module is called first time
            val cameraResult = googleMapsViewModel.googleMapViewActionFlow.first()
            val expectedCamera = getDefaultCamera()
            assertEquals(expectedCamera, cameraResult)

            // Verify that right message is called
            val messageResult = googleMapsViewModel.googleMapViewActionFlow.first()
            assertEquals(GoogleMapsViewModel.Messages.NO_RESTAURANT, messageResult)

            googleMapsViewModel.googleMapViewStateLiveData.observeForever {
                // Then
                // Verify that mapData sends the right stuff
                val expectedResult =
                    getDefaultGoogleMapViewState(restaurantViewStateList = emptyList())
                assertEquals(expectedResult, it)
            }
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

    private fun getDefaultGoogleMapViewState(
        restaurantViewStateList: List<GoogleMapsViewModel.RestaurantViewState> = getDefaultRestaurantViewStateList()
    ) = GoogleMapsViewModel.GoogleMapViewState(
        LATITUDE.toDouble(),
        LONGITUDE.toDouble(),
        ZOOM.toFloat(),
        restaurantViewStateList
    )

    private fun getDefaultRestaurantViewStateList() =
        listOf(
            GoogleMapsViewModel.RestaurantViewState(
                latitude = LATITUDE.toDouble(),
                longitude = LONGITUDE.toDouble(),
                name = NAME,
                id = PLACE_ID,
                iconDrawable = R.drawable.grey_pin_128px
            )
        )

    private fun getDefaultCamera(
        camera: GoogleMapsViewModel.GoogleMapViewAction = GoogleMapsViewModel.GoogleMapViewAction.MoveCamera(
            getDefaultCameraUpdateFactory()
        )
    ) = camera

    private fun getDefaultCameraUpdateFactory() =
        CameraUpdateFactory.newLatLngZoom(
            LatLng(
                LATITUDE.toDouble(),
                LONGITUDE.toDouble()
            ),
            ZOOM.toFloat()
        )

    // endregion
}