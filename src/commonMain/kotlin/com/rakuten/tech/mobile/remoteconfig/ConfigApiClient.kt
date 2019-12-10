package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.header
import io.ktor.client.response.HttpResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.Transient

internal expect val ApplicationDispatcher: CoroutineDispatcher

class ConfigApiClient internal constructor(
    platformClient: HttpClient,
    private val baseUrl: String,
    private val appId: String,
    subscriptionKey: String,
    deviceModel: String,
    osVersion: String,
    appName: String,
    appVersion: String,
    sdkVersion: String,
    private val scope: CoroutineScope
) {

    constructor(
        platformClient: HttpClient,
        baseUrl: String,
        appId: String,
        subscriptionKey: String,
        deviceModel: String,
        osVersion: String,
        appName: String,
        appVersion: String,
        sdkVersion: String
    ) : this (
        platformClient = platformClient,
        baseUrl = baseUrl,
        appId = appId,
        subscriptionKey = subscriptionKey,
        deviceModel = deviceModel,
        osVersion = osVersion,
        appName = appName,
        appVersion = appVersion,
        sdkVersion = sdkVersion,
        scope = CoroutineScope(ApplicationDispatcher)
    )

    private val client = platformClient.config {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        defaultRequest {
            header("apiKey" , "ras-$subscriptionKey")
            header("ras-app-id", appId)
            header("ras-device-model", deviceModel)
            header("ras-os-version", osVersion)
            header("ras-sdk-name", "Remote Config")
            header("ras-sdk-version", sdkVersion)
            header("ras-app-name", appName)
            header("ras-app-version", appVersion)
        }
    }

    fun fetchConfig(
        success: (Config) -> Unit,
        error: (Exception) -> Unit
    ) {
        scope.launch {
            try {
                val config = ConfigRequest(
                    httpClient = client,
                    baseUrl = baseUrl,
                    appId = appId
                ).fetch()
                success(config)
            } catch (exception: ResponseException) {
                error(exception)
            }
        }
    }

    fun fetchPublicKey(
        keyId: String,
        success: (String) -> Unit,
        error: (Exception) -> Unit
    ) {
        scope.launch {
            try {
                val key = PublicKeyRequest(
                    httpClient = client,
                    baseUrl = baseUrl
                ).fetch(keyId)
                success(key)
            } catch (exception: ResponseException) {
                error(exception)
            }
        }
    }
}

/**
 * Base for default response exceptions.
 * @param response: origin response
 */
open class ResponseException(
    @Transient val response: HttpResponse
) : IllegalStateException("Bad response: ${response.status}")
