package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryRange
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyError
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyResult
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStatus
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CoinMarketCapRemoteDataSourceTest {
    @Test
    fun historyBypassesTheLocalRateLimiterWhileOtherEndpointsRemainLimited() = runTest {
        val requestedPaths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requestedPaths += request.url.encodedPath
            respondJson(HISTORY_JSON)
        }) {
            expectSuccess = false
            install(ContentNegotiation) { json(cryptoNetworkJson) }
        }
        val service = Ktorfit.Builder()
            .baseUrl("https://pro-api.coinmarketcap.com/")
            .httpClient(client)
            .converterFactories(FlowConverterFactory(), ResponseConverterFactory())
            .build()
            .createCoinMarketCapService()
        val limiter = ApiRateLimiter(
            nowMillis = { 1_000L },
            wait = { awaitCancellation() },
        )
        limiter.acquire(requestsPerMinute = 1)
        val remote = CoinMarketCapRemoteDataSource(
            service = service,
            requestExecutor = CoinMarketCapRequestExecutor(),
            authenticatedExecutor = AuthenticatedRequestExecutor(FakeSecureApiKeyStorage),
            rateLimiter = limiter,
        ).also { it.updateRateLimit(1) }

        val history = withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                remote.coinHistory(1, CoinHistoryRange.HOURS_24).first()
            }
        }
        val limitedQuote = withContext(Dispatchers.Default) {
            withTimeoutOrNull(100) {
                remote.coinQuotes(listOf(1)).first()
            }
        }

        assertIs<ApiResult.Success<*>>(history)
        assertNull(limitedQuote)
        assertEquals(listOf("/v3/cryptocurrency/quotes/historical"), requestedPaths)
        client.close()
    }

    private fun MockRequestHandleScope.respondJson(body: String): HttpResponseData = respond(
        content = body,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private data object FakeSecureApiKeyStorage : SecureApiKeyStorage {
        override suspend fun save(apiKey: String) = SecureApiKeyResult.Success(Unit)
        override suspend fun read() = SecureApiKeyResult.Success("test-api-key")
        override suspend fun status() = SecureApiKeyStatus.CONFIGURED
        override suspend fun remove() = SecureApiKeyResult.Failure(SecureApiKeyError.NOT_CONFIGURED)
    }

    private companion object {
        const val HISTORY_JSON = """
            {
              "status":{"timestamp":"2026-08-26T00:00:00.000Z","error_code":0,"elapsed":1,"credit_count":1},
              "data":{"1":{"quotes":[{"timestamp":"2026-08-26T00:00:00.000Z","quote":{"USD":{"price":100.0}}}]}}
            }
        """
    }
}
