package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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

class CoinPaprikaRemoteDataSourceTest {
    @Test
    fun `search uses currency and symbol filters without authentication`() = runTest {
        var captured: HttpRequestData? = null
        val fixture = createFixture { request ->
            captured = request
            respondJson(SEARCH_JSON)
        }

        val result = fixture.remote.searchCoins("BTC").first()

        val success = assertIs<ApiResult.Success<*>>(result, result.toString())
        assertEquals(1, (success.data as List<*>).size)
        assertEquals("/v1/search", captured?.url?.encodedPath)
        assertEquals("BTC", captured?.url?.parameters?.get("q"))
        assertEquals("currencies", captured?.url?.parameters?.get("c"))
        assertEquals("symbol_search", captured?.url?.parameters?.get("modifier"))
        assertEquals("250", captured?.url?.parameters?.get("limit"))
        assertNull(captured?.headers?.get("X-CMC_PRO_API_KEY"))
        assertNull(captured?.headers?.get(HttpHeaders.Authorization))
        fixture.client.close()
    }

    @Test
    fun `coin detail performs only the detail request`() = runTest {
        var requestCount = 0
        var path: String? = null
        val fixture = createFixture { request ->
            requestCount += 1
            path = request.url.encodedPath
            respondJson(COIN_JSON)
        }

        val result = fixture.remote.coinInformation("btc-bitcoin").first()

        assertIs<ApiResult.Success<*>>(result, result.toString())
        assertEquals(1, requestCount)
        assertEquals("/v1/coins/btc-bitcoin", path)
        fixture.client.close()
    }

    @Test
    fun `not found response is explicit`() = runTest {
        val fixture = createFixture {
            respondJson("""{"error":"id not found"}""", HttpStatusCode.NotFound)
        }

        val result = fixture.remote.coinInformation("missing-coin").first()

        val failure = assertIs<ApiResult.Failure>(result)
        assertIs<CryptoError.NotFound>(failure.error)
        fixture.client.close()
    }

    @Test
    fun `coin detail bypasses rate limiter while search remains limited`() = runTest {
        val requestedPaths = mutableListOf<String>()
        val limiter = ApiRateLimiter(
            nowMillis = { 1_000L },
            wait = { awaitCancellation() },
        )
        repeat(600) { limiter.acquire(requestsPerMinute = 600) }
        val fixture = createFixture(rateLimiter = limiter) { request ->
            requestedPaths += request.url.encodedPath
            respondJson(COIN_JSON)
        }

        val information = withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                fixture.remote.coinInformation("btc-bitcoin").first()
            }
        }
        val limitedSearch = withContext(Dispatchers.Default) {
            withTimeoutOrNull(100) {
                fixture.remote.searchCoins("BTC").first()
            }
        }

        assertIs<ApiResult.Success<*>>(information)
        assertNull(limitedSearch)
        assertEquals(listOf("/v1/coins/btc-bitcoin"), requestedPaths)
        fixture.client.close()
    }

    private fun createFixture(
        rateLimiter: ApiRateLimiter = ApiRateLimiter(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): Fixture {
        val client = HttpClient(MockEngine { request -> handler(request) }) {
            expectSuccess = false
            install(ContentNegotiation) { json(cryptoNetworkJson) }
        }
        val service = Ktorfit.Builder()
            .baseUrl("https://api.coinpaprika.com/v1/")
            .httpClient(client)
            .converterFactories(FlowConverterFactory(), ResponseConverterFactory())
            .build()
            .createCoinPaprikaService()
        return Fixture(
            remote = CoinPaprikaRemoteDataSource(
                service = service,
                requestExecutor = CoinPaprikaRequestExecutor(),
                rateLimiter = rateLimiter,
            ),
            client = client,
        )
    }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private data class Fixture(
        val remote: CoinPaprikaRemoteDataSource,
        val client: HttpClient,
    )

    private companion object {
        const val SEARCH_JSON = """
            {"currencies":[{"id":"btc-bitcoin","name":"Bitcoin","symbol":"BTC","rank":1,"is_active":true,"type":"coin"}]}
        """
        const val COIN_JSON = """
            {"id":"btc-bitcoin","name":"Bitcoin","symbol":"BTC","rank":1,"is_active":true,"type":"coin","description":"Bitcoin","links":{"website":["https://bitcoin.org/"]}}
        """
    }
}
