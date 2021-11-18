package fr.plopez.go4lunch.tests

import fr.plopez.go4lunch.data.RestaurantDAO
import fr.plopez.go4lunch.data.model.restaurant.*
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantEntity
import fr.plopez.go4lunch.data.model.restaurant.entites.RestaurantsQuery
import fr.plopez.go4lunch.data.model.restaurant.entites.relations.QueryWithRestaurants
import fr.plopez.go4lunch.data.repositories.RestaurantsRepository
import fr.plopez.go4lunch.di.CoroutinesProvider
import fr.plopez.go4lunch.di.NearbyConstants
import fr.plopez.go4lunch.retrofit.RestaurantService
import fr.plopez.go4lunch.tests.utils.CommonsUtils
import fr.plopez.go4lunch.tests.utils.CommonsUtils.ERROR_CODE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LATITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LOCATION
import fr.plopez.go4lunch.tests.utils.CommonsUtils.LONGITUDE
import fr.plopez.go4lunch.tests.utils.CommonsUtils.MAX_DISPLACEMENT_TOL
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PERIODS_SEARCH_FIELD
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_PHONE_NUMBER
import fr.plopez.go4lunch.tests.utils.CommonsUtils.QUERY_TIME_STAMP
import fr.plopez.go4lunch.tests.utils.CommonsUtils.RADIUS
import fr.plopez.go4lunch.tests.utils.CommonsUtils.PLACE_WEBSITE
import fr.plopez.go4lunch.utils.TestCoroutineRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import uk.co.jemos.podam.api.PodamFactoryImpl
import java.io.IOException
import kotlin.math.round

@ExperimentalCoroutinesApi
class RestaurantsRepositoryTest {

    // Rules
    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    // Mocks
    private val restaurantServiceMock = mockk<RestaurantService>()
    private val restaurantsCacheDAOMock = mockk<RestaurantDAO>()
    private val coroutinesProviderMock = mockk<CoroutinesProvider>()

    // Test variables
    private val nearbyConstants = NearbyConstants()
    private val podamFactory = PodamFactoryImpl()
    private val restaurantQueryResponseItem =
        podamFactory.manufacturePojo(RestaurantQueryResponseItem::class.java)
    private val queryWithRestaurants = getQueryWithRestaurants()

    private lateinit var restaurantsRepository: RestaurantsRepository

    @Before
    fun setUp() {
        // Coroutines provider mock - provides a specific dispatcher for tests
        every { coroutinesProviderMock.ioCoroutineDispatcher } returns testCoroutineRule.testCoroutineDispatcher

        // service mockk
        coEvery {
            restaurantServiceMock.getDetailsForRestaurant(
                nearbyConstants.key,
                PERIODS_SEARCH_FIELD,
                any()
            )
        } returns Response.success(
            DetailsQueryResult(
                html_attributions = emptyList(),
                result = Result(
                    opening_hours = OpeningHours(
                        open_now = true,
                        periods = CommonsUtils.getDefaultPeriodList(),
                        weekday_text = listOf("")
                    ),
                    phone_number = PLACE_PHONE_NUMBER,
                    website = PLACE_WEBSITE
                ),
                status = ""
            )
        )

        // DAO mockk
        coJustRun { restaurantsCacheDAOMock.upsertQuery(any()) }
        coJustRun { restaurantsCacheDAOMock.upsertRestaurant(any()) }
        coJustRun { restaurantsCacheDAOMock.upsertRestaurantOpeningPeriod(any()) }
        coJustRun { restaurantsCacheDAOMock.upsertRestaurantOpeningPeriodCrossReference(any()) }
        coJustRun { restaurantsCacheDAOMock.upsertRestaurantQueriesCrossReference(any()) }

        coEvery {
            restaurantsCacheDAOMock.getNearestRestaurants(
                latitude = LATITUDE.toDouble(),
                longitude = LONGITUDE.toDouble(),
                displacementTol = MAX_DISPLACEMENT_TOL.toFloat()
            )
        } returns emptyList()

        coEvery {
            restaurantServiceMock.getNearbyRestaurants(
                nearbyConstants.key,
                nearbyConstants.type,
                LOCATION,
                RADIUS
            )
        } returns Response.success(
            NearbyQueryResult(
                html_attributions = emptyList(),
                next_page_token = "",
                results = listOf(restaurantQueryResponseItem),
                ""
            )
        )

        restaurantsRepository = RestaurantsRepository(
            restaurantService = restaurantServiceMock,
            nearbyConstants = nearbyConstants,
            restaurantsCacheDAO = restaurantsCacheDAOMock,
            coroutinesProvider = coroutinesProviderMock
        )

    }

    // returns some restaurants from the network because database is empty
    // or do not contains the corresponding query
    @Test
    fun `getRestaurantsAroundPosition from network case`() = testCoroutineRule.runBlockingTest {

        // Given

        // When
        val result = restaurantsRepository.getRestaurantsAroundPosition(
            latitude = LATITUDE,
            longitude = LONGITUDE,
            radius = RADIUS
        ).first()

        // Then
        val expectedResponse = RestaurantsRepository.ResponseStatus.Success(
            getExpectedRestaurantEntityList(restaurantQueryResponseItem)
        )
        assertEquals(expectedResponse, result)
        coVerify(exactly = 1) { restaurantsCacheDAOMock.upsertQuery(any()) }
        coVerify(atLeast = 1) { restaurantsCacheDAOMock.upsertRestaurant(any()) }
        coVerify(atLeast = 1) { restaurantsCacheDAOMock.upsertRestaurantQueriesCrossReference(any()) }
        coVerify(atLeast = 1) { restaurantsCacheDAOMock.upsertRestaurantOpeningPeriod(any()) }
        coVerify(atLeast = 1) {
            restaurantsCacheDAOMock.upsertRestaurantOpeningPeriodCrossReference(
                any()
            )
        }
    }

    // returns some restaurants from the database
    @Test
    fun `getRestaurantsAroundPosition from database case`() = testCoroutineRule.runBlockingTest {

        // Given
        coEvery {
            restaurantsCacheDAOMock.getNearestRestaurants(
                latitude = LATITUDE.toDouble(),
                longitude = LONGITUDE.toDouble(),
                displacementTol = MAX_DISPLACEMENT_TOL.toFloat()
            )
        } returns queryWithRestaurants

        // When
        val result = restaurantsRepository.getRestaurantsAroundPosition(
            latitude = LATITUDE,
            longitude = LONGITUDE,
            radius = RADIUS
        ).first()

        // Then
        val expectedResponse = RestaurantsRepository.ResponseStatus.Success(
            queryWithRestaurants.first().restaurantList
        )
        assertEquals(expectedResponse, result)
        coVerify(exactly = 0) { restaurantsCacheDAOMock.upsertQuery(any()) }
        coVerify(exactly = 0) { restaurantsCacheDAOMock.upsertRestaurant(any()) }
        coVerify(exactly = 0) { restaurantsCacheDAOMock.upsertRestaurantQueriesCrossReference(any()) }
        coVerify(exactly = 0) { restaurantsCacheDAOMock.upsertRestaurantOpeningPeriod(any()) }
        coVerify(exactly = 0) {
            restaurantsCacheDAOMock.upsertRestaurantOpeningPeriodCrossReference(
                any()
            )
        }
    }

    // returns exception message because IOException
    @Test
    fun `getRestaurantsAroundPosition IOException`() =
        testCoroutineRule.runBlockingTest {

            // Given
            coEvery {
                restaurantServiceMock.getNearbyRestaurants(
                    nearbyConstants.key,
                    nearbyConstants.type,
                    LOCATION,
                    RADIUS
                )
            }.throws(IOException("this is a fake IO exception for testing purpose"))

            // When
            val result = restaurantsRepository.getRestaurantsAroundPosition(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                radius = RADIUS
            ).first()

            // Then
            val expectedResponse = RestaurantsRepository.ResponseStatus.StatusError.IOException
            assertEquals(expectedResponse, result)
        }

    // returns exception message because HttpException
    @Test
    fun `getRestaurantsAroundPosition HttpException`() =
        testCoroutineRule.runBlockingTest {

            // Given
            coEvery {
                restaurantServiceMock.getNearbyRestaurants(
                    nearbyConstants.key,
                    nearbyConstants.type,
                    LOCATION,
                    RADIUS
                )
            }.throws(
                HttpException(
                    Response.error<Any>(
                        ERROR_CODE,
                        ResponseBody.create(
                            MediaType.parse("plain/text"), "some content"
                        )
                    )
                )
            )

            // When
            val result = restaurantsRepository.getRestaurantsAroundPosition(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                radius = RADIUS
            ).first()

            // Then
            val expectedResponse = RestaurantsRepository.ResponseStatus.StatusError.HttpException
            assertEquals(expectedResponse, result)
        }

    // returns no response message because request not successful
    @Test
    fun `getRestaurantsAroundPosition no response because request not successful`() =
        testCoroutineRule.runBlockingTest {

            // Given
            coEvery {
                restaurantServiceMock.getNearbyRestaurants(
                    nearbyConstants.key,
                    nearbyConstants.type,
                    LOCATION,
                    RADIUS
                )
            } returns Response.error(
                ERROR_CODE,
                ResponseBody.create(
                    MediaType.parse("plain/text"), "some content"
                )
            )

            // When
            val result = restaurantsRepository.getRestaurantsAroundPosition(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                radius = RADIUS
            ).first()

            // Then
            val expectedResponse = RestaurantsRepository.ResponseStatus.NoResponse
            assertEquals(expectedResponse, result)
        }

    // returns no response message because request result is empty
    @Test
    fun `getRestaurantsAroundPosition no response because request result is empty`() =
        testCoroutineRule.runBlockingTest {

            // Given
            coEvery {
                restaurantServiceMock.getNearbyRestaurants(
                    nearbyConstants.key,
                    nearbyConstants.type,
                    LOCATION,
                    RADIUS
                )
            } returns Response.success(
                NearbyQueryResult(
                    html_attributions = emptyList(),
                    next_page_token = "",
                    results = emptyList(),
                    ""
                )
            )

            // When
            val result = restaurantsRepository.getRestaurantsAroundPosition(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                radius = RADIUS
            ).first()

            // Then
            val expectedResponse = RestaurantsRepository.ResponseStatus.NoRestaurants
            assertEquals(expectedResponse, result)
        }

    // returns no response message because request result is null
    @Test
    fun `getRestaurantsAroundPosition no response because request result is null`() =
        testCoroutineRule.runBlockingTest {

            // Given
            coEvery {
                restaurantServiceMock.getNearbyRestaurants(
                    nearbyConstants.key,
                    nearbyConstants.type,
                    LOCATION,
                    RADIUS
                )
            } returns Response.success(null)

            // When
            val result = restaurantsRepository.getRestaurantsAroundPosition(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                radius = RADIUS
            ).first()

            // Then
            val expectedResponse = RestaurantsRepository.ResponseStatus.NoResponse
            assertEquals(expectedResponse, result)
        }

    // Verify that no periods are inserted in database if no data is available
    @Test
    fun `opening periods not inserted in database when empty`() =
        testCoroutineRule.runBlockingTest {

            // Given
            coEvery {
                restaurantServiceMock.getDetailsForRestaurant(
                    nearbyConstants.key,
                    PERIODS_SEARCH_FIELD,
                    any()
                )
            } returns Response.success(
                DetailsQueryResult(
                    html_attributions = emptyList(),
                    result = Result(
                        opening_hours = OpeningHours(
                            open_now = true,
                            periods = emptyList(),
                            weekday_text = listOf("")
                        ),
                        phone_number = PLACE_PHONE_NUMBER,
                        website = PLACE_WEBSITE

                    ),
                    status = ""
                )
            )

            // When
            val result = restaurantsRepository.getRestaurantsAroundPosition(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                radius = RADIUS
            ).first()

            // then
            val expectedResponse = RestaurantsRepository.ResponseStatus.Success(
                getExpectedRestaurantEntityList(restaurantQueryResponseItem)
            )
            assertEquals(expectedResponse, result)
            coVerify(exactly = 0) { restaurantsCacheDAOMock.upsertRestaurantOpeningPeriod(any()) }
            coVerify(exactly = 0) {
                restaurantsCacheDAOMock.upsertRestaurantOpeningPeriodCrossReference(
                    any()
                )
            }
        }

    // Verify that no periods are inserted in database if no data is available
    @Test
    fun `opening periods not inserted in database when null`() =
        testCoroutineRule.runBlockingTest {

            // Given
            coEvery {
                restaurantServiceMock.getDetailsForRestaurant(
                    nearbyConstants.key,
                    PERIODS_SEARCH_FIELD,
                    any()
                )
            } returns Response.success(null)

            // When
            val result = restaurantsRepository.getRestaurantsAroundPosition(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                radius = RADIUS
            ).first()

            // then
            val expectedResponse = RestaurantsRepository.ResponseStatus.Success(
                getExpectedRestaurantEntityListNoDetails(restaurantQueryResponseItem)
            )
            assertEquals(expectedResponse, result)
            coVerify(exactly = 0) { restaurantsCacheDAOMock.upsertRestaurantOpeningPeriod(any()) }
            coVerify(exactly = 0) {
                restaurantsCacheDAOMock.upsertRestaurantOpeningPeriodCrossReference(
                    any()
                )
            }
        }

    // Verify that no periods are inserted in database if no data is available
    @Test
    fun `opening periods not inserted in database when httpException`() =
        testCoroutineRule.runBlockingTest {

            // Given
            coEvery {
                restaurantServiceMock.getDetailsForRestaurant(
                    nearbyConstants.key,
                    PERIODS_SEARCH_FIELD,
                    any()
                )
            }.throws(
                HttpException(
                    Response.error<Any>(
                        ERROR_CODE,
                        ResponseBody.create(
                            MediaType.parse("plain/text"), "some content"
                        )
                    )
                )
            )

            // When
            val result = restaurantsRepository.getRestaurantsAroundPosition(
                latitude = LATITUDE,
                longitude = LONGITUDE,
                radius = RADIUS
            ).first()

            // then
            val expectedResponse = RestaurantsRepository.ResponseStatus.Success(
                getExpectedRestaurantEntityListNoDetails(restaurantQueryResponseItem)
            )
            assertEquals(expectedResponse, result)
            coVerify(exactly = 0) { restaurantsCacheDAOMock.upsertRestaurantOpeningPeriod(any()) }
            coVerify(exactly = 0) {
                restaurantsCacheDAOMock.upsertRestaurantOpeningPeriodCrossReference(
                    any()
                )
            }
        }

    // region IN
    private fun getExpectedRestaurantEntityList(
        restaurantQueryResponseItem: RestaurantQueryResponseItem
    ): List<RestaurantEntity> {
        return listOf(
            RestaurantEntity(
                restaurantId = restaurantQueryResponseItem.placeID!!,
                name = restaurantQueryResponseItem.name!!,
                address = restaurantQueryResponseItem.vicinity!!,
                latitude = restaurantQueryResponseItem.geometry!!.location!!.lat!!,
                longitude = restaurantQueryResponseItem.geometry!!.location!!.lng!!,
                photoUrl = restaurantQueryResponseItem.photos?.firstOrNull()?.photoReference,
                rate = round((restaurantQueryResponseItem.rating!!.toFloat() * 3.0f) / 5.0f),
                phoneNumber = PLACE_PHONE_NUMBER,
                website = PLACE_WEBSITE
            )
        )
    }

    private fun getExpectedRestaurantEntityListNoDetails(
        restaurantQueryResponseItem: RestaurantQueryResponseItem
    ): List<RestaurantEntity> {
        return listOf(
            RestaurantEntity(
                restaurantId = restaurantQueryResponseItem.placeID!!,
                name = restaurantQueryResponseItem.name!!,
                address = restaurantQueryResponseItem.vicinity!!,
                latitude = restaurantQueryResponseItem.geometry!!.location!!.lat!!,
                longitude = restaurantQueryResponseItem.geometry!!.location!!.lng!!,
                photoUrl = restaurantQueryResponseItem.photos?.firstOrNull()?.photoReference,
                rate = round((restaurantQueryResponseItem.rating!!.toFloat() * 3.0f) / 5.0f),
                phoneNumber = "",
                website = ""
            )
        )
    }

    private fun getQueryWithRestaurants(): List<QueryWithRestaurants> {

        val restaurantsQuery = RestaurantsQuery(
            queryTimeStamp = QUERY_TIME_STAMP,
            latitude = LATITUDE.toDouble(),
            longitude = LONGITUDE.toDouble()
        )

        return listOf(
            QueryWithRestaurants(
                query = restaurantsQuery,
                restaurantList = getExpectedRestaurantEntityList(
                    restaurantQueryResponseItem
                )
            )
        )
    }
    // endregion
}

