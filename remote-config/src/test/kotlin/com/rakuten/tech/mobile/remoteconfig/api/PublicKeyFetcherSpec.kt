package com.rakuten.tech.mobile.remoteconfig.api

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.argForWhich
import com.nhaarman.mockitokotlin2.eq
import com.rakuten.tech.mobile.sdkutils.RasSdkHeaders
import junit.framework.TestCase
import okhttp3.*
import org.amshove.kluent.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

open class PublicKeyFetcherSpec {
    internal val mockApiClient: ConfigApiClient = mock()

    internal fun createFetcher() =
        PublicKeyFetcher(mockApiClient)

    internal fun enqueueResponse(
        body: String,
        code: Int
    ) {
        When calling mockApiClient.fetchPath(any(), eq(null)) itReturns Response.Builder()
            .request(Request.Builder().url("https://www.example.com").build())
            .protocol(Protocol.HTTP_2)
            .message("")
            .code(code)
            .body(ResponseBody.create(MediaType.get("text/plain; charset=utf-8"), body))
            .build()
    }
}

class PublicKeyFetcherNormalSpec : PublicKeyFetcherSpec() {

    private fun enqueueSuccessResponse(
        id: String = "test_id",
        key: String = "test_key",
        createdAt: String = "2019-07-23T07:24:57+00:00"
    ) {
        enqueueResponse(
            body = """
                {
                    "id": "$id",
                    "key": "$key",
                    "createdAt": "$createdAt"
                }
            """.trimIndent(),
            code = 200
        )
    }

    @Test
    fun `should fetch the public key`() {
        val fetcher = createFetcher()
        enqueueSuccessResponse(key = "test_key")

        fetcher.fetch("test_key_id") shouldEqual "test_key"
    }

    @Test
    fun `should fetch the public key for the provided key id`() {
        val fetcher = createFetcher()
        enqueueSuccessResponse(id = "test_key_id")

        fetcher.fetch("test_key_id")

        Verify on mockApiClient that mockApiClient.fetchPath(argForWhich {
            contains("keys/test_key_id")
        }, eq(null))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PublicKeyFetcherErrorSpec : PublicKeyFetcherSpec() {

    private fun enqueueErrorResponse(
        body: String = "",
        code: Int = 200
    ) {
        enqueueResponse(
            body = body,
            code = code
        )
    }

    @Test(expected = IOException::class)
    fun `should throw when the request is unsuccessful`() {
        enqueueErrorResponse(code = 400)

        val fetcher = createFetcher()

        fetcher.fetch("test_key_id")
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'id' key is missing in response`() {
        enqueueErrorResponse(
            body = """{
                "key": "test_key",
                "createdAt": "test_created"
            }""".trimIndent()
        )
        val fetcher = createFetcher()

        fetcher.fetch("test_key_id")
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'key' key is missing in response`() {
        enqueueErrorResponse(
            body = """{
                "id": "test_key_id",
                "createdAt": "test_created"
            }""".trimIndent()
        )
        val fetcher = createFetcher()

        fetcher.fetch("test_key_id")
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'createdAt' key is missing in response`() {
        enqueueErrorResponse(
            body = """{
                "id": "test_key_id",
                "key": "test_key"
            }""".trimIndent()
        )
        val fetcher = createFetcher()

        fetcher.fetch("test_key_id")
    }

    @Test(expected = IOException::class)
    fun `should throw when valid client but invalid url`() {
        val mockRasHeaders: RasSdkHeaders = mock()
        When calling mockRasHeaders.asArray() itReturns emptyArray()
        enqueueErrorResponse(code = 404)

        val fetcher = PublicKeyFetcher(ConfigApiClient(
                "https://www.example.com", ApplicationProvider.getApplicationContext(), mockRasHeaders))

        fetcher.fetch("test_key_id")
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun `should not throw when there are extra keys in the response`() {
        enqueueErrorResponse(
            body = """{
                "id": "test_id",
                "key": "test_key",
                "createdAt": "test_created",
                "randomKey": "random_value"
            }""".trimIndent()
        )
        val fetcher = createFetcher()

        try {
            fetcher.fetch("test_key_id")
        } catch (e: Exception) {
            TestCase.fail("Should not throw an exception.")
            throw e
        }
    }
}
