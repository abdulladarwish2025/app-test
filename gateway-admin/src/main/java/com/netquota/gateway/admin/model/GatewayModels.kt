package com.netquota.gateway.admin.model

import kotlinx.serialization.Serializable

@Serializable
data class GatewayStatus(
    val name: String = "NetQuota Box",
    val online: Boolean = false,
    val mode: String = "demo",
    val uptimeSeconds: Long = 0,
    val wanOnline: Boolean = false,
    val version: String = "0.1.0"
)

@Serializable
data class ManagedDevice(
    val id: String,
    val name: String,
    val owner: String = "غير محدد",
    val kind: String = "device",
    val ipAddress: String = "",
    val macAddress: String = "",
    val quotaBytes: Long,
    val usedBytes: Long,
    val downloadBytes: Long = 0,
    val uploadBytes: Long = 0,
    val downloadBps: Long = 0,
    val uploadBps: Long = 0,
    val paused: Boolean = false,
    val online: Boolean = true,
    val resetAt: String = "00:00"
) {
    val progress: Float
        get() = if (quotaBytes <= 0) 0f else (usedBytes.toDouble() / quotaBytes).toFloat().coerceIn(0f, 1f)

    val remainingBytes: Long
        get() = (quotaBytes - usedBytes).coerceAtLeast(0)

    val exhausted: Boolean
        get() = quotaBytes > 0 && usedBytes >= quotaBytes
}

@Serializable
data class DevicesResponse(val devices: List<ManagedDevice>)

data class GatewaySnapshot(
    val status: GatewayStatus,
    val devices: List<ManagedDevice>
)

@Serializable
data class BonusRequest(val bytes: Long)

@Serializable
data class QuotaRequest(val limitBytes: Long)

data class GatewayConnection(
    val baseUrl: String = "",
    val token: String = ""
)
