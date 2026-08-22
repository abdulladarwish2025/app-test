package com.netquota.gateway.admin.data

import android.content.Context
import com.netquota.gateway.admin.model.GatewayConnection

class AdminPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("gateway_admin", Context.MODE_PRIVATE)

    fun loadConnection(): GatewayConnection = GatewayConnection(
        baseUrl = preferences.getString(KEY_BASE_URL, "").orEmpty(),
        token = preferences.getString(KEY_TOKEN, "").orEmpty()
    )

    fun saveConnection(connection: GatewayConnection) {
        preferences.edit()
            .putString(KEY_BASE_URL, connection.baseUrl)
            .putString(KEY_TOKEN, connection.token)
            .apply()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
    }
}
