package fr.plopez.go4lunch.tests

import fr.plopez.go4lunch.data.RestaurantDAO
import fr.plopez.go4lunch.data.model.restaurant.NearbyQueryResult
import fr.plopez.go4lunch.data.model.restaurant.RestaurantQueryResponseItem
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.QueryWithRestaurants
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.di.NearbyConstants
import fr.plopez.go4lunch.retrofit.RestaurantService
import fr.plopez.go4lunch.utils.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.any
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import uk.co.jemos.podam.api.PodamFactoryImpl

@ExperimentalCoroutinesApi
class RestaurantsRepositoryTest {

    // Constants
    companion object {
        private const val QUERY_ID = 66666666
        private const val LATITUDE = "90.0"
        private const val LONGITUDE = "0.0"
        private const val LOCATION = "$LATITUDE,$LONGITUDE"
        private const val RADIUS = "1000"
        private const val MAX_DISPLACEMENT_TOL = "0.000449"


        private val SUCCESS_RESTAURANT_ENTITY_LIST = emptyList<RestaurantEntity>()
        private val EMPTY_QUERY_WITH_RESTAURANTS = emptyList<QueryWithRestaurants>()
//        private val POPULATED_QUERY_WITH_RESTAURANTS = QueryWithRestaurants(
//
//        )
    }

    // Rules
    @Rule
    val testCoroutineRule = TestCoroutineRule()

    // Mocks
    private val restaurantServiceMock = mockk<RestaurantService>()
    private val nearbyConstants = NearbyConstants()
    private val restaurantsCacheDAOMock = mockk<RestaurantDAO>()
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()
    private val podamFactory = PodamFactoryImpl()

    // Test variables

    @Before
    fun setUp() {
        // Coroutines provider mock - provides a specific dispatcher for tests
        every { coroutinesProviderMock.ioCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher

        // service mockk

    }

    @Test
    // returns some restaurants from the network because database is empty
    // or do not contains a corresponding query
    fun `getRestaurantsAroundPosition nominal case`() = testCoroutineRule.runBlockingTest {
        // Given
        coEvery {restaurantsCacheDAOMock.getNearestRestaurants(
            latitude = LATITUDE.toDouble(),
            longitude = LONGITUDE.toDouble(),
            displacementTol = MAX_DISPLACEMENT_TOL.toFloat()
        )} returns emptyList()

        coEvery { restaurantServiceMock.getNearbyRestaurants(
            nearbyConstants.key,
            nearbyConstants.type,
            LOCATION,
            RADIUS
        ) } returns Response.success(
            NearbyQueryResult(
                html_attributions = emptyList(),
                next_page_token = "",
                results = listOf(podamFactory.manufacturePojo(RestaurantQueryResponseItem::class.java)),
                ""
            )
        )

        val restaurantsRepository = buildRestaurantRepository()

        // When
        val result = restaurantsRepository.getRestaurantsAroundPosition(
            latitude = LATITUDE,
            longitude = LONGITUDE,
            radius = RADIUS
        ).first()

        // Then
        assertEquals(getSuccessResponse(), result)
    }

    private fun getSuccessResponse(): RestaurantsRepository.ResponseStatus {
        return RestaurantsRepository.ResponseStatus.Success(SUCCESS_RESTAURANT_ENTITY_LIST)
    }

    private fun buildRestaurantRepository(): RestaurantsRepository{
        return RestaurantsRepository(
            restaurantService = restaurantServiceMock,
            nearbyConstants = nearbyConstants,
            restaurantsCacheDAO = restaurantsCacheDAOMock,
            coroutinesProvider = coroutinesProviderMock
        )
    }

}

