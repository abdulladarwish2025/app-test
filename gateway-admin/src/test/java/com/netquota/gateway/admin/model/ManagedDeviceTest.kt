package com.netquota.gateway.admin.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedDeviceTest {
    @Test
    fun progressIsClampedAndRemainingNeverNegative() {
        val device = ManagedDevice(
            id = "d1",
            name = "Test",
            quotaBytes = 1_000,
            usedBytes = 1_250
        )

        assertEquals(1f, device.progress)
        assertEquals(0, device.remainingBytes)
        assertTrue(device.exhausted)
    }
}
