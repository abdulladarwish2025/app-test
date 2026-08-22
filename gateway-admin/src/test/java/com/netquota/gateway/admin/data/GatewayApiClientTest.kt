package com.netquota.gateway.admin.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GatewayApiClientTest {
    @Test
    fun normalizesLocalGatewayAddress() {
        assertEquals("http://192.168.50.1:8787", normalizeBaseUrl("192.168.50.1:8787/"))
        assertEquals("https://gateway.local", normalizeBaseUrl("https://gateway.local/"))
    }
}
