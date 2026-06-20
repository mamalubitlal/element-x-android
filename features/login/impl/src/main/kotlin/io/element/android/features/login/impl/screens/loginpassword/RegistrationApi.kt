/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.loginpassword

import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Inject
class RegistrationApi {
    data class RegistrationResult(
        val accessToken: String,
        val deviceId: String,
        val userId: String,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Register a new account using [m.login.dummy] auth on the given [baseUrl].
     *
     * Follows the Matrix spec:
     * 1. POST `/_matrix/client/v3/register` with `m.login.dummy` auth
     * 2. If server returns a session (requires auth), POST again with the session
     */
    suspend fun register(
        baseUrl: String,
        username: String,
        password: String,
        initialDeviceName: String?,
    ): Result<RegistrationResult> {
        return runCatching {
            val registerUrl = baseUrl.trimEnd('/') + "/_matrix/client/v3/register"

            // First attempt: try registration with m.login.dummy
            val body = buildRegisterBody(username, password, initialDeviceName, session = null)
            val responseText = httpPost(registerUrl, body)
            val responseJson = json.parseToJsonElement(responseText).jsonObject

            // Check if registration completed (has access_token)
            responseJson["access_token"]?.let { token ->
                return@runCatching RegistrationResult(
                    accessToken = token.jsonPrimitive.content,
                    deviceId = responseJson["device_id"]?.jsonPrimitive?.content.orEmpty(),
                    userId = responseJson["user_id"]?.jsonPrimitive?.content.orEmpty(),
                )
            }

            // If we got a session, complete the registration
            val session = responseJson["session"]?.jsonPrimitive?.content
                ?: error("Registration failed: no access_token and no session in response")

            val bodyWithAuth = buildRegisterBody(username, password, initialDeviceName, session = session)
            val finalResponseText = httpPost(registerUrl, bodyWithAuth)
            val finalJson = json.parseToJsonElement(finalResponseText).jsonObject

            RegistrationResult(
                accessToken = finalJson["access_token"]?.jsonPrimitive?.content
                    ?: error("Registration failed: no access_token in final response"),
                deviceId = finalJson["device_id"]?.jsonPrimitive?.content.orEmpty(),
                userId = finalJson["user_id"]?.jsonPrimitive?.content.orEmpty(),
            )
        }
    }

    private fun buildRegisterBody(
        username: String,
        password: String,
        initialDeviceName: String?,
        session: String?,
    ): String {
        val auth = if (session != null) {
            buildJsonObject {
                put("type", "m.login.dummy")
                put("session", session)
            }
        } else {
            buildJsonObject {
                put("type", "m.login.dummy")
            }
        }
        return buildJsonObject {
            put("username", username)
            put("password", password)
            put("auth", auth)
            if (!initialDeviceName.isNullOrBlank()) {
                put("initial_device_display_name", initialDeviceName)
            }
        }.toString()
    }

    private fun httpPost(url: String, body: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return try {
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }
            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            responseStream.bufferedReader().use { it.readText() }.also {
                if (responseCode !in 200..299) {
                    throw RuntimeException("Registration HTTP $responseCode")
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
