package com.netquota.gateway.admin.data

import com.netquota.gateway.admin.model.BonusRequest
import com.netquota.gateway.admin.model.DevicesResponse
import com.netquota.gateway.admin.model.GatewayConnection
import com.netquota.gateway.admin.model.GatewaySnapshot
import com.netquota.gateway.admin.model.GatewayStatus
import com.netquota.gateway.admin.model.ManagedDevice
import com.netquota.gateway.admin.model.QuotaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GatewayApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun snapshot(connection: GatewayConnection): GatewaySnapshot = withContext(Dispatchers.IO) {
        val status = get<GatewayStatus>(connection, "/api/v1/status")
        val devices = get<DevicesResponse>(connection, "/api/v1/devices").devices
        GatewaySnapshot(status, devices)
    }

    suspend fun setPaused(connection: GatewayConnection, deviceId: String, paused: Boolean): ManagedDevice =
        post(connection, "/api/v1/devices/$deviceId/${if (paused) "pause" else "resume"}", "{}")

    suspend fun addBonus(connection: GatewayConnection, deviceId: String, bytes: Long): ManagedDevice =
        post(connection, "/api/v1/devices/$deviceId/bonus", json.encodeToString(BonusRequest(bytes)))

    suspend fun setQuota(connection: GatewayConnection, deviceId: String, bytes: Long): ManagedDevice =
        put(connection, "/api/v1/devices/$deviceId/quota", json.encodeToString(QuotaRequest(bytes)))

    private suspend inline fun <reified T> get(connection: GatewayConnection, path: String): T =
        execute(connection, path, "GET", null)

    private suspend inline fun <reified T> post(
        connection: GatewayConnection,
        path: String,
        body: String
    ): T = execute(connection, path, "POST", body)

    private suspend inline fun <reified T> put(
        connection: GatewayConnection,
        path: String,
        body: String
    ): T = execute(connection, path, "PUT", body)

    private suspend inline fun <reified T> execute(
        connection: GatewayConnection,
        path: String,
        method: String,
        body: String?
    ): T = withContext(Dispatchers.IO) {
        val url = normalizeBaseUrl(connection.baseUrl) + path
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        if (connection.token.isNotBlank()) builder.header("Authorization", "Bearer ${connection.token}")
        val requestBody = body?.toRequestBody(JSON_MEDIA_TYPE)
        builder.method(method, requestBody)

        client.newCall(builder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GatewayApiException("فشل الاتصال (${response.code})", response.code)
            }
            json.decodeFromString<T>(responseBody)
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

class GatewayApiException(message: String, val statusCode: Int) : Exception(message)

fun normalizeBaseUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isBlank()) throw IllegalArgumentException("اكتب عنوان NetQuota Box")
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
}
