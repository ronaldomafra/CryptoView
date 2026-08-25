package br.com.rmf.kmp.cryptoview.domain.usecase

import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRequestExecutor
import br.com.rmf.kmp.cryptoview.data.api.configureCryptoHttpClient
import br.com.rmf.kmp.cryptoview.data.api.createCoinMarketCapService
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.repository.CoinMarketCapDataRepository
import br.com.rmf.kmp.cryptoview.utils.defaultCryptoProcessConfig
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetKeyInfoUseCaseTest {
    @Test
    fun validResponseFlowsFromServiceThroughRepositoryAndUseCase() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val fixture = createFixture { request ->
            capturedRequest = request
            respondJson(VALID_KEY_INFO_JSON)
        }

        val result = fixture.execute("top-secret-key")

        val success = assertIs<ApiResult.Success<CoinMarketCapKeyInfo>>(result)
        assertEquals(10_000, success.data.plan?.creditLimitMonthly)
        assertEquals(29, success.data.usage?.currentMinute?.requestsLeft)
        assertEquals(1, success.metadata.creditCount)
        assertEquals("top-secret-key", capturedRequest?.headers?.get(API_KEY_HEADER))
        assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))
        assertEquals("/v1/key/info", capturedRequest?.url?.encodedPath)
        fixture.close()
    }

    @Test
    fun blankApiKeyIsRejectedByUseCaseWithoutExecutingARequest() = runBlocking {
        var requestCount = 0
        val fixture = createFixture {
            requestCount++
            respondJson(VALID_KEY_INFO_JSON)
        }

        val result = fixture.execute("   ")

        assertEquals(ApiResult.Failure(CryptoError.MissingApiKey), result)
        assertEquals(0, requestCount)
        fixture.close()
    }

    @Test
    fun optionalAndUnknownResponseFieldsAreAccepted() = runBlocking {
        val fixture = createFixture {
            respondJson(
                """
                {
                  "data": {"plan": {"unknown": true}, "future_field": "ignored"},
                  "status": {"error_code": 0, "another_future_field": 42}
                }
                """.trimIndent(),
            )
        }

        val success = assertIs<ApiResult.Success<CoinMarketCapKeyInfo>>(fixture.execute("key"))

        assertNull(success.data.plan?.creditLimitMonthly)
        assertNull(success.data.usage)
        fixture.close()
    }

    @Test
    fun businessErrorInsideSuccessfulHttpResponseIsMapped() = runBlocking {
        val fixture = createFixture {
            respondJson(apiErrorJson(code = 1001, message = "API key invalid"))
        }

        val failure = assertIs<ApiResult.Failure>(fixture.execute("bad-key"))

        assertIs<CryptoError.InvalidApiKey>(failure.error)
        fixture.close()
    }

    @Test
    fun authenticationAndPlanErrorsAreMapped() = runBlocking {
        val unauthorized = createFixture {
            respondJson(apiErrorJson(1001, "invalid"), HttpStatusCode.Unauthorized)
        }
        val forbidden = createFixture {
            respondJson(apiErrorJson(1006, "plan"), HttpStatusCode.Forbidden)
        }

        assertIs<CryptoError.InvalidApiKey>(
            assertIs<ApiResult.Failure>(unauthorized.execute("key")).error,
        )
        assertIs<CryptoError.PlanUnavailable>(
            assertIs<ApiResult.Failure>(forbidden.execute("key")).error,
        )
        unauthorized.close()
        forbidden.close()
    }

    @Test
    fun rateLimitAndServerErrorsPreserveRelevantMetadata() = runBlocking {
        val rateLimited = createFixture {
            respondJson(
                body = "not-json",
                status = HttpStatusCode.TooManyRequests,
                extraHeaders = headersOf(HttpHeaders.RetryAfter, "17"),
            )
        }
        val unavailable = createFixture {
            respondJson("{}", HttpStatusCode.ServiceUnavailable)
        }

        val rateError = assertIs<CryptoError.RateLimited>(
            assertIs<ApiResult.Failure>(rateLimited.execute("key")).error,
        )
        assertEquals("17", rateError.retryAfterRaw)
        assertEquals(17_000, rateError.retryAfterMillis)
        assertEquals(
            CryptoError.ServerUnavailable(503),
            assertIs<ApiResult.Failure>(unavailable.execute("key")).error,
        )
        rateLimited.close()
        unavailable.close()
    }

    @Test
    fun invalidDataAndMalformedJsonHaveDistinctFailures() = runBlocking {
        val missingData = createFixture {
            respondJson("""{"status":{"error_code":0}}""")
        }
        val malformed = createFixture {
            respondJson("not-json")
        }

        assertIs<CryptoError.InvalidResponse>(
            assertIs<ApiResult.Failure>(missingData.execute("key")).error,
        )
        assertIs<CryptoError.Serialization>(
            assertIs<ApiResult.Failure>(malformed.execute("key")).error,
        )
        missingData.close()
        malformed.close()
    }

    @Test
    fun connectionAndTimeoutFailuresAreMapped() = runBlocking {
        val offline = createFixture { throw IOException("offline") }
        val timeout = createFixture { request -> throw HttpRequestTimeoutException(request) }

        assertEquals(ApiResult.Failure(CryptoError.NoConnection), offline.execute("key"))
        assertEquals(ApiResult.Failure(CryptoError.Timeout), timeout.execute("key"))
        offline.close()
        timeout.close()
    }

    @Test
    fun cancellationIsRethrown() = runBlocking {
        val fixture = createFixture { throw CancellationException("cancelled") }

        assertFailsWith<CancellationException> { fixture.execute("key") }
        fixture.close()
    }

    @Test
    fun apiKeyIsNeverWrittenToLogs() = runBlocking {
        val logs = mutableListOf<String>()
        val fixture = createFixture(
            logger = object : Logger {
                override fun log(message: String) {
                    logs += message
                }
            },
        ) {
            respondJson(VALID_KEY_INFO_JSON)
        }

        fixture.execute("must-never-be-logged")

        assertTrue(logs.isNotEmpty())
        assertTrue(logs.none { "must-never-be-logged" in it })
        fixture.close()
    }

    private fun createFixture(
        logger: Logger = object : Logger {
            override fun log(message: String) = Unit
        },
        handler: suspend MockRequestHandleScope.(HttpRequestData) ->
            io.ktor.client.request.HttpResponseData,
    ): Fixture {
        val engine = MockEngine { request -> handler(request) }
        val httpClient = HttpClient(engine) {
            configureCryptoHttpClient(
                config = defaultCryptoProcessConfig(
                    databaseWriteParallelism = 1,
                    databasePoolSize = 1,
                ),
                networkLogger = logger,
            )
        }
        val service = Ktorfit.Builder()
            .baseUrl("https://pro-api.coinmarketcap.com/")
            .httpClient(httpClient)
            .converterFactories(
                FlowConverterFactory(),
                ResponseConverterFactory(),
            )
            .build()
            .createCoinMarketCapService()
        val useCase = GetKeyInfoUseCase(
            service = service,
            requestExecutor = CoinMarketCapRequestExecutor(),
        )
        return Fixture(
            repository = CoinMarketCapDataRepository(useCase),
            httpClient = httpClient,
        )
    }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        extraHeaders: io.ktor.http.Headers = headersOf(),
    ) = respond(
        content = body,
        status = status,
        headers = io.ktor.http.Headers.build {
            append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            appendAll(extraHeaders)
        },
    )

    private fun apiErrorJson(code: Int, message: String) =
        """{"status":{"error_code":$code,"error_message":"$message"}}"""

    private data class Fixture(
        val repository: CoinMarketCapDataRepository,
        val httpClient: HttpClient,
    ) {
        suspend fun execute(apiKey: String) =
            repository.getKeyInfo(apiKey).single()

        fun close() = httpClient.close()
    }

    private companion object {
        const val API_KEY_HEADER = "X-CMC_PRO_API_KEY"
        val VALID_KEY_INFO_JSON =
            """
            {
              "data": {
                "plan": {
                  "credit_limit_monthly": 10000,
                  "credit_limit_monthly_reset": "Every 30 days",
                  "credit_limit_monthly_reset_timestamp": "2026-09-01T00:00:00.000Z",
                  "rate_limit_minute": 30
                },
                "usage": {
                  "current_minute": {"requests_made": 1, "requests_left": 29},
                  "current_day": {"credits_used": 12, "credits_left": 988},
                  "current_month": {"credits_used": 100, "credits_left": 9900}
                }
              },
              "status": {
                "timestamp": "2026-08-24T12:00:00.000Z",
                "error_code": 0,
                "error_message": null,
                "elapsed": 5,
                "credit_count": 1,
                "notice": null
              }
            }
            """.trimIndent()
    }
}
