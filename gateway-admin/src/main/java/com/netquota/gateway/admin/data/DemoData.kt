package com.netquota.gateway.admin.data

import com.netquota.gateway.admin.model.GatewaySnapshot
import com.netquota.gateway.admin.model.GatewayStatus
import com.netquota.gateway.admin.model.ManagedDevice

object DemoData {
    private const val GB = 1024L * 1024L * 1024L

    fun snapshot() = GatewaySnapshot(
        status = GatewayStatus(
            name = "NetQuota Box — المنزل",
            online = true,
            mode = "demo",
            uptimeSeconds = 184_320,
            wanOnline = true
        ),
        devices = listOf(
            ManagedDevice(
                id = "phone-mariam",
                name = "هاتف مريم",
                owner = "مريم",
                kind = "phone",
                ipAddress = "192.168.50.21",
                macAddress = "8A:21:4F:10:31:90",
                quotaBytes = 5 * GB,
                usedBytes = 3 * GB + 260 * 1024L * 1024L,
                downloadBytes = 3 * GB,
                uploadBytes = 260 * 1024L * 1024L,
                downloadBps = 1_450_000,
                uploadBps = 94_000
            ),
            ManagedDevice(
                id = "tv-living",
                name = "تلفزيون الصالة",
                owner = "المنزل",
                kind = "tv",
                ipAddress = "192.168.50.30",
                macAddress = "72:9C:00:25:AF:11",
                quotaBytes = 8 * GB,
                usedBytes = 2 * GB + 610 * 1024L * 1024L,
                downloadBytes = 2 * GB + 590 * 1024L * 1024L,
                uploadBytes = 20 * 1024L * 1024L,
                downloadBps = 4_800_000,
                uploadBps = 28_000
            ),
            ManagedDevice(
                id = "laptop-omar",
                name = "لابتوب عمر",
                owner = "عمر",
                kind = "laptop",
                ipAddress = "192.168.50.42",
                macAddress = "3E:B2:18:4C:71:A0",
                quotaBytes = 3 * GB,
                usedBytes = 3 * GB,
                downloadBytes = 2 * GB + 900 * 1024L * 1024L,
                uploadBytes = 124 * 1024L * 1024L,
                paused = true,
                downloadBps = 0,
                uploadBps = 0
            )
        )
    )
}
