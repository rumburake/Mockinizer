package com.appham.mockinizer

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.security.cert.X509Certificate
import javax.net.ssl.*


/**
 * The main function that wires up the [MockWebServer] with [OkHttpClient]. Generally only the
 * mocks map needs to be defined. The default values for the other params should be fine for most
 * projects.
 *
 * @param mocks Map of RequestFilter / MockResponse Entries to define requests that
 * should be directed to the mock server instead of the real one. default value is an empty map.
 * @param trustManagers Array of TrustManager to be used for https connections with mock server
 * default value is an all trusting manager
 * @param socketFactory SSLSocketFactory to be used for RetroFit https connections
 * default value is using the previously defined trustManagers
 * @param hostnameVerifier HostNameVerifier the interface to be used to verify hostnames.
 * default value is an all verifying verifier
 *
 * @return OkHttpClient.Builder for chaining
 */
fun OkHttpClient.Builder.mockinize(
    mocks: Map<RequestFilter, MockResponse> = mapOf(),
    mockWebServer: MockWebServer = MockWebServer().configure(),
    trustManagers: Array<TrustManager> = getAllTrustingManagers(),
    socketFactory: SSLSocketFactory = getSslSocketFactory(trustManagers),
    hostnameVerifier: HostnameVerifier = HostnameVerifier { _, _ -> true },
    log: Logger = DebugLogger
): OkHttpClient.Builder {
    addInterceptor(MockinizerInterceptor(mocks, mockWebServer))
        .sslSocketFactory(socketFactory, trustManagers[0] as X509TrustManager)
        .hostnameVerifier(hostnameVerifier)
    Mockinizer.init(mockWebServer, mocks)

    log.d("Mockinized $this with mocks: $mocks and MockWebServer $mockWebServer")

    return this
}

private fun getSslSocketFactory(trustManagers: Array<TrustManager>): SSLSocketFactory =
    SSLContext.getInstance("SSL").apply {
        init(null, trustManagers, java.security.SecureRandom())
    }.socketFactory

private fun getAllTrustingManagers(): Array<TrustManager> = arrayOf(
    object : X509TrustManager {

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

        override fun checkClientTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) {
        }

        override fun checkServerTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) {
        }
    }
)

internal class MockDispatcher(private val mocks: Map<RequestFilter, MockResponse>) : Dispatcher() {

    @Throws(InterruptedException::class)
    override fun dispatch(request: RecordedRequest): MockResponse {
        return with(RequestFilter.from(request)) {
            mocks[RequestFilter.from(request)]
                ?: mocks[copy(body = null)]
                ?: mocks[copy(headers = request.headers.withClearedOkhttpHeaders())]
                ?: mocks[copy(headers = null)]
                ?: mocks[copy(body = null, headers = request.headers.withClearedOkhttpHeaders())]
                ?: mocks[copy(body = null, headers = null)]
                ?: MockResponse().setResponseCode(404)
        }
    }

    /**
     * Removes headers that OkHttp would add to RecordedRequest
     */
    private fun Headers.withClearedOkhttpHeaders() =
        if (
            get(":authority")?.startsWith("localhost:") == true &&
            get(":scheme")?.matches("https?".toRegex()) == true &&
            get("accept-encoding") == "gzip" &&
            get("user-agent")?.startsWith("okhttp/") == true
        ) {
            newBuilder()
                .removeAll(":authority")
                .removeAll(":scheme")
                .removeAll("accept-encoding")
                .removeAll("user-agent")
                .build()
        } else {
            this
        }
}

object Mockinizer {

    internal var mockWebServer: MockWebServer? = null

    internal fun init(
        mockWebServer: MockWebServer,
        mocks: Map<RequestFilter, MockResponse>
    ) {

        mocks.entries.forEach { (requestFilter, mockResponse) ->
            mockResponse.setHeader(
                "Mockinizer",
                " <-- Real request ${requestFilter.path} is now mocked to $mockResponse"
            )
            mockResponse.setHeader(
                "server",
                "Mockinizer ${BuildConfig.VERSION_NAME} by Thomas Fuchs-Martin"
            )
        }

        mockWebServer.dispatcher = MockDispatcher(mocks)

        this.mockWebServer = mockWebServer

    }

    @JvmStatic
    fun start(port: Int = 34567) {
        mockWebServer?.start(port)
    }

    @JvmStatic
    fun shutDown() {
        mockWebServer?.shutdown()
    }

}
