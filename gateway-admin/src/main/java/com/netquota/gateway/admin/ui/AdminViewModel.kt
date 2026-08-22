package com.netquota.gateway.admin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netquota.gateway.admin.data.AdminPreferences
import com.netquota.gateway.admin.data.DemoData
import com.netquota.gateway.admin.data.GatewayApiClient
import com.netquota.gateway.admin.model.GatewayConnection
import com.netquota.gateway.admin.model.GatewaySnapshot
import com.netquota.gateway.admin.model.ManagedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminTab { DASHBOARD, DEVICES, SETTINGS }

data class AdminUiState(
    val snapshot: GatewaySnapshot = DemoData.snapshot(),
    val connection: GatewayConnection = GatewayConnection(),
    val activeTab: AdminTab = AdminTab.DASHBOARD,
    val isRefreshing: Boolean = false,
    val isDemo: Boolean = true,
    val errorMessage: String? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AdminPreferences(application)
    private val apiClient = GatewayApiClient()
    private val savedConnection = preferences.loadConnection()

    private val _uiState = MutableStateFlow(
        AdminUiState(
            connection = savedConnection,
            isDemo = savedConnection.baseUrl.isBlank()
        )
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        if (savedConnection.baseUrl.isNotBlank()) refresh()
    }

    fun selectTab(tab: AdminTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateConnectionDraft(baseUrl: String? = null, token: String? = null) {
        _uiState.update { state ->
            state.copy(
                connection = state.connection.copy(
                    baseUrl = baseUrl ?: state.connection.baseUrl,
                    token = token ?: state.connection.token
                ),
                errorMessage = null
            )
        }
    }

    fun connect() {
        val connection = _uiState.value.connection
        preferences.saveConnection(connection)
        refresh(forceConnection = connection)
    }

    fun useDemoMode() {
        preferences.saveConnection(GatewayConnection())
        _uiState.update {
            it.copy(
                snapshot = DemoData.snapshot(),
                connection = GatewayConnection(),
                isDemo = true,
                isRefreshing = false,
                errorMessage = null,
                lastUpdatedAt = System.currentTimeMillis(),
                activeTab = AdminTab.DASHBOARD
            )
        }
    }

    fun refresh(forceConnection: GatewayConnection? = null) {
        val connection = forceConnection ?: _uiState.value.connection
        if (connection.baseUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    snapshot = DemoData.snapshot(),
                    isDemo = true,
                    isRefreshing = false,
                    errorMessage = null,
                    lastUpdatedAt = System.currentTimeMillis()
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            runCatching { apiClient.snapshot(connection) }
                .onSuccess { snapshot ->
                    preferences.saveConnection(connection)
                    _uiState.update {
                        it.copy(
                            snapshot = snapshot,
                            connection = connection,
                            isDemo = false,
                            isRefreshing = false,
                            errorMessage = null,
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = error.message ?: "تعذر الوصول إلى NetQuota Box"
                        )
                    }
                }
        }
    }

    fun togglePause(device: ManagedDevice) {
        if (_uiState.value.isDemo) {
            replaceDevice(device.copy(paused = !device.paused, downloadBps = 0, uploadBps = 0))
            return
        }
        runDeviceAction { apiClient.setPaused(_uiState.value.connection, device.id, !device.paused) }
    }

    fun addBonus(device: ManagedDevice, bytes: Long = 512L * 1024L * 1024L) {
        if (_uiState.value.isDemo) {
            replaceDevice(device.copy(quotaBytes = device.quotaBytes + bytes, paused = false))
            return
        }
        runDeviceAction { apiClient.addBonus(_uiState.value.connection, device.id, bytes) }
    }

    fun setQuota(device: ManagedDevice, bytes: Long) {
        if (_uiState.value.isDemo) {
            replaceDevice(device.copy(quotaBytes = bytes, paused = device.usedBytes >= bytes))
            return
        }
        runDeviceAction { apiClient.setQuota(_uiState.value.connection, device.id, bytes) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun runDeviceAction(action: suspend () -> ManagedDevice) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            runCatching { action() }
                .onSuccess { device ->
                    replaceDevice(device)
                    _uiState.update { it.copy(isRefreshing = false, lastUpdatedAt = System.currentTimeMillis()) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = error.message ?: "تعذر تنفيذ الأمر"
                        )
                    }
                }
        }
    }

    private fun replaceDevice(device: ManagedDevice) {
        _uiState.update { state ->
            state.copy(
                snapshot = state.snapshot.copy(
                    devices = state.snapshot.devices.map { if (it.id == device.id) device else it }
                ),
                lastUpdatedAt = System.currentTimeMillis()
            )
        }
    }
}
