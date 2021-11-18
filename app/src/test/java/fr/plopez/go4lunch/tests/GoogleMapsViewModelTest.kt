package fr.plopez.go4lunch.tests

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.repositories.FirestoreRepository
import fr.plopez.go4lunch.data.repositories.LocationRepository
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LATITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LONGITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_ID
import fr.plopez.go4lunch.tests.utils.CommonsUtils.RADIUS
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_EMAIL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_NAME
import fr.plopez.go4lunch.tests.utils.CommonsUtils.WORKMATE_PHOTO_URL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.ZOOM
import fr.plopez.go4lunch.tests.utils.CommonsUtils.getDefaultRestaurantEntityList
import fr.plopez.go4lunch.tests.utils.LiveDataUtils.getOrAwaitValue
import fr.plopez.go4lunch.utils.TestCoroutineRule
import fr.plopez.go4lunch.view.main_activity.google_maps.GoogleMapsViewModel
import fr.plopez.go4lunch.view.model.WorkmateWithSelectedRestaurant
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class GoogleMapsViewModelTest {

    companion object {
        private const val NO_RESTAURANT = R.string.no_restaurants_message
        private const val NO_RESPONSE = R.string.no_response_message
        private const val NO_INTERNET = R.string.network_error_message
        private const val NETWORK_ERROR = R.string.no_internet_message
    }

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()
    private val locationRepositoryMockK = mockk<LocationRepository>()
    private val restaurantsRepositoryMockK = mockk<RestaurantsRepository>()
    private val firestoreRepositoryMockk = mockk<FirestoreRepository>()
    private val contextMockK = mockk<Context>()

    // Test variables

    @Before
    fun setUp() {
        // Coroutines provider mock - provides a specific dispatcher for tests
        every { coroutinesProviderMock.ioCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher
        every { coroutinesProviderMock.mainCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher

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

        // Mock Context
        every {
            contextMockK.resources.getString(R.string.default_detection_radius_value)
        } returns RADIUS
    }

    @Test
    fun `success response when a list of restaurant is returned and a restaurant is selected by users`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val googleMapsViewModel = GoogleMapsViewModel(
                locationRepository = locationRepositoryMockK,
                restaurantsRepository = restaurantsRepositoryMockK,
                firestoreRepository = firestoreRepositoryMockk,
                coroutinesProvider = coroutinesProviderMock,
                context = contextMockK
            )

            // When
            googleMapsViewModel.onMapReady()
            googleMapsViewModel.googleMapViewStateLiveData.observeForever {
                // Then
                assertEquals(getDefaultGoogleMapViewState(), it)
            }
        }

    @Test
    fun `success response when a list of restaurant is returned and a restaurant is not selected by users`() =

        testCoroutineRule.runBlockingTest {
            // Given
            val googleMapsViewModel = GoogleMapsViewModel(
                locationRepository = locationRepositoryMockK,
                restaurantsRepository = restaurantsRepositoryMockK,
                firestoreRepository = firestoreRepositoryMockk,
                coroutinesProvider = coroutinesProviderMock,
                context = contextMockK
            )

            coEvery {
                firestoreRepositoryMockk.getWorkmatesWithSelectedRestaurants()
            } returns flowOf(
                emptyList()
            )

            // When
            googleMapsViewModel.onMapReady()
            googleMapsViewModel.googleMapViewStateLiveData.observeForever {
                // Then
                assertEquals(getDefaultGoogleMapViewState(
                    restaurantViewStateList = listOf(
                        GoogleMapsViewModel.RestaurantViewState(
                            latitude = LATITUDE.toDouble(),
                            longitude = LONGITUDE.toDouble(),
                            name = PLACE_NAME,
                            id = PLACE_ID,
                            iconDrawable = R.drawable.grey_pin_128px
                        )
                    )
                ), it)
            }
        }

    @Test
    fun `NoRestaurant response when list of restaurant is empty`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val googleMapsViewModel = GoogleMapsViewModel(
                locationRepository = locationRepositoryMockK,
                restaurantsRepository = restaurantsRepositoryMockK,
                firestoreRepository = firestoreRepositoryMockk,
                coroutinesProvider = coroutinesProviderMock,
                context = contextMockK
            )

            coEvery {
                restaurantsRepositoryMockK.getRestaurantsAroundPosition(
                    LATITUDE,
                    LONGITUDE,
                    RADIUS
                )
            } returns flowOf(
                RestaurantsRepository.ResponseStatus.NoRestaurants
            )
            val observer = mockk<Observer<GoogleMapsViewModel.GoogleMapViewAction>> {
                every { onChanged(any()) } just Runs
            }
            googleMapsViewModel.googleMapViewActionLiveData.observeForever(observer)

            // When
            // Activate the switch map
            googleMapsViewModel.onMapReady()
            // Activate live data
            googleMapsViewModel.googleMapViewStateLiveData.getOrAwaitValue()

            // Then
            verifySequence {
                // Verify that right camera module is called first time
                observer.onChanged(getDefaultCamera())
                // Verify that right message is called
                observer.onChanged(
                    GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage(
                        NO_RESTAURANT
                    )
                )
            }
        }

    @Test
    fun `NoRestaurant response trigger only one time when list of restaurant is empty many times`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val googleMapsViewModel = GoogleMapsViewModel(
                locationRepository = locationRepositoryMockK,
                restaurantsRepository = restaurantsRepositoryMockK,
                firestoreRepository = firestoreRepositoryMockk,
                coroutinesProvider = coroutinesProviderMock,
                context = contextMockK
            )

            coEvery {
                restaurantsRepositoryMockK.getRestaurantsAroundPosition(
                    LATITUDE,
                    LONGITUDE,
                    RADIUS
                )
            } returns flowOf(
                RestaurantsRepository.ResponseStatus.NoRestaurants,
                RestaurantsRepository.ResponseStatus.NoRestaurants,
                RestaurantsRepository.ResponseStatus.NoRestaurants
            )

            val observer = mockk<Observer<GoogleMapsViewModel.GoogleMapViewAction>> {
                every { onChanged(any()) } just Runs
            }
            googleMapsViewModel.googleMapViewActionLiveData.observeForever(observer)

            // When
            // Activate the switch map
            googleMapsViewModel.onMapReady()
            // Activate live data
            googleMapsViewModel.googleMapViewStateLiveData.getOrAwaitValue()

            // Then
            verifySequence {
                // Verify that right camera module is called first time
                observer.onChanged(getDefaultCamera())
                // Verify that right message is called
                observer.onChanged(
                    GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage(
                        NO_RESTAURANT
                    )
                )
            }
        }

    @Test
    fun `http error message when http exception is returned`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val googleMapsViewModel = GoogleMapsViewModel(
                locationRepository = locationRepositoryMockK,
                restaurantsRepository = restaurantsRepositoryMockK,
                firestoreRepository = firestoreRepositoryMockk,
                coroutinesProvider = coroutinesProviderMock,
                context = contextMockK
            )
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
            // Activate the switch map
            googleMapsViewModel.onMapReady()

            // Then
            val expectedResult =
                GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage(NETWORK_ERROR)
            googleMapsViewModel.googleMapViewActionLiveData.observeForever {
                assertEquals(expectedResult, it)
            }
        }

    @Test
    fun `no response message when nothing is returned`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val googleMapsViewModel = GoogleMapsViewModel(
                locationRepository = locationRepositoryMockK,
                restaurantsRepository = restaurantsRepositoryMockK,
                firestoreRepository = firestoreRepositoryMockk,
                coroutinesProvider = coroutinesProviderMock,
                context = contextMockK
            )
            coEvery {
                restaurantsRepositoryMockK.getRestaurantsAroundPosition(
                    LATITUDE,
                    LONGITUDE,
                    RADIUS
                )
            } returns flowOf()
            // When
            // Activate the switch map
            googleMapsViewModel.onMapReady()

            // Then
            val expectedResult =
                GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage(NO_RESPONSE)
            googleMapsViewModel.googleMapViewActionLiveData.observeForever {
                assertEquals(expectedResult, it)
            }
        }

    @Test
    fun `internet error message when io exception is returned`() =
        testCoroutineRule.runBlockingTest {
            // Given
            val googleMapsViewModel = GoogleMapsViewModel(
                locationRepository = locationRepositoryMockK,
                restaurantsRepository = restaurantsRepositoryMockK,
                firestoreRepository = firestoreRepositoryMockk,
                coroutinesProvider = coroutinesProviderMock,
                context = contextMockK
            )
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
            // Activate the switch map
            googleMapsViewModel.onMapReady()

            // Then
            val expectedResult =
                GoogleMapsViewModel.GoogleMapViewAction.ResponseStatusMessage(NO_INTERNET)
            googleMapsViewModel.googleMapViewActionLiveData.observeForever {
                assertEquals(expectedResult, it)
            }
        }

    // region IN

    private fun getDefaultGoogleMapViewState(
        restaurantViewStateList: List<GoogleMapsViewModel.RestaurantViewState> = getDefaultRestaurantViewStateList()
    ) = GoogleMapsViewModel.GoogleMapViewState(
        latitude = LATITUDE.toDouble(),
        longitude =  LONGITUDE.toDouble(),
        zoom = ZOOM.toFloat(),
        restaurantList = restaurantViewStateList
    )

    private fun getDefaultRestaurantViewStateList() =
        listOf(
            GoogleMapsViewModel.RestaurantViewState(
                latitude = LATITUDE.toDouble(),
                longitude = LONGITUDE.toDouble(),
                name = PLACE_NAME,
                id = PLACE_ID,
                iconDrawable = R.drawable.grey_pin_star_128px
            )
        )

    private fun getDefaultCamera(
        camera: GoogleMapsViewModel.GoogleMapViewAction =
            GoogleMapsViewModel.GoogleMapViewAction.MoveCamera(
                latLng = LatLng(
                    LATITUDE.toDouble(),
                    LONGITUDE.toDouble()
                ),
                zoom = ZOOM.toFloat()
            )
    ) = camera

    // endregion
}