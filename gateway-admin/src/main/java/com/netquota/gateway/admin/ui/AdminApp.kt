package com.netquota.gateway.admin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.netquota.gateway.admin.model.ManagedDevice
import com.netquota.gateway.admin.ui.theme.Amber
import com.netquota.gateway.admin.ui.theme.Danger
import com.netquota.gateway.admin.ui.theme.DeepInk
import com.netquota.gateway.admin.ui.theme.ElectricSky
import com.netquota.gateway.admin.ui.theme.Muted
import com.netquota.gateway.admin.ui.theme.SignalBlue
import com.netquota.gateway.admin.ui.theme.Success
import java.util.Locale

@Composable
fun AdminApp(viewModel: AdminViewModel) {
    val state by viewModel.uiState.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                AdminTopBar(
                    isOnline = state.snapshot.status.online,
                    isDemo = state.isDemo,
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh
                )
            },
            bottomBar = {
                AdminNavigation(
                    activeTab = state.activeTab,
                    onSelect = viewModel::selectTab
                )
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                AnimatedVisibility(state.errorMessage != null) {
                    ErrorBanner(
                        message = state.errorMessage.orEmpty(),
                        onDismiss = viewModel::dismissError
                    )
                }

                when (state.activeTab) {
                    AdminTab.DASHBOARD -> DashboardScreen(
                        state = state,
                        onTogglePause = viewModel::togglePause,
                        onAddBonus = viewModel::addBonus
                    )

                    AdminTab.DEVICES -> DevicesScreen(
                        devices = state.snapshot.devices,
                        onTogglePause = viewModel::togglePause,
                        onAddBonus = viewModel::addBonus,
                        onSetQuota = viewModel::setQuota
                    )

                    AdminTab.SETTINGS -> SettingsScreen(
                        state = state,
                        onUrlChange = { viewModel.updateConnectionDraft(baseUrl = it) },
                        onTokenChange = { viewModel.updateConnectionDraft(token = it) },
                        onConnect = viewModel::connect,
                        onUseDemo = viewModel::useDemoMode
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTopBar(
    isOnline: Boolean,
    isDemo: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text("NETQUOTA", style = MaterialTheme.typography.labelSmall, color = ElectricSky)
                Text("إدارة الشبكة", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 14.dp, end = 8.dp)
                    .size(42.dp)
                    .background(ElectricSky.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Router, contentDescription = null, tint = ElectricSky)
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = if (isDemo) "تجريبي" else if (isOnline) "متصل" else "غير متصل",
                    color = if (isDemo) Amber else if (isOnline) Success else Danger
                )
                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = ElectricSky,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Refresh, "تحديث", tint = Color.White)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepInk)
    )
}

@Composable
private fun AdminNavigation(activeTab: AdminTab, onSelect: (AdminTab) -> Unit) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationItem(AdminTab.DASHBOARD, activeTab, Icons.Rounded.Dashboard, "الرئيسية", onSelect)
        NavigationItem(AdminTab.DEVICES, activeTab, Icons.Rounded.Devices, "الأجهزة", onSelect)
        NavigationItem(AdminTab.SETTINGS, activeTab, Icons.Rounded.Settings, "الإعدادات", onSelect)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavigationItem(
    tab: AdminTab,
    activeTab: AdminTab,
    icon: ImageVector,
    label: String,
    onSelect: (AdminTab) -> Unit
) {
    NavigationBarItem(
        selected = tab == activeTab,
        onClick = { onSelect(tab) },
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) }
    )
}

@Composable
private fun DashboardScreen(
    state: AdminUiState,
    onTogglePause: (ManagedDevice) -> Unit,
    onAddBonus: (ManagedDevice) -> Unit
) {
    val devices = state.snapshot.devices
    val totalUsed = devices.sumOf { it.usedBytes }
    val totalQuota = devices.sumOf { it.quotaBytes }
    val activeCount = devices.count { it.online && !it.paused }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 16.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NetworkHero(
                gatewayName = state.snapshot.status.name,
                activeDevices = activeCount,
                totalDevices = devices.size,
                usedBytes = totalUsed,
                quotaBytes = totalQuota,
                online = state.snapshot.status.wanOnline
            )
        }
        item {
            SectionHeading("أجهزة تحتاج انتباهك", "القرار الأقرب أولًا")
        }
        items(
            devices.sortedWith(compareByDescending<ManagedDevice> { it.paused || it.exhausted }.thenByDescending { it.progress }),
            key = { it.id }
        ) { device ->
            DeviceOrbitCard(device, onTogglePause, onAddBonus)
        }
    }
}

@Composable
private fun NetworkHero(
    gatewayName: String,
    activeDevices: Int,
    totalDevices: Int,
    usedBytes: Long,
    quotaBytes: Long,
    online: Boolean
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DeepInk)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(DeepInk, Color(0xFF164C85))
                    )
                )
                .padding(22.dp)
        ) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width * 0.18f, size.height * 0.42f)
                val points = listOf(
                    Offset(size.width * 0.05f, size.height * 0.78f),
                    Offset(size.width * 0.31f, size.height * 0.17f),
                    Offset(size.width * 0.43f, size.height * 0.71f)
                )
                points.forEach { point ->
                    drawLine(ElectricSky.copy(alpha = 0.13f), center, point, 2f)
                    drawCircle(ElectricSky.copy(alpha = 0.2f), 9f, point)
                }
                drawCircle(ElectricSky.copy(alpha = 0.15f), 44f, center, style = Stroke(3f))
                drawCircle(ElectricSky.copy(alpha = 0.55f), 8f, center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("الشبكة تحت سيطرتك", style = MaterialTheme.typography.displaySmall, color = Color.White)
                        Text(gatewayName, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                    }
                    Icon(
                        if (online) Icons.Rounded.CheckCircle else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = if (online) ElectricSky else Danger,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroMetric("المستهلك اليوم", formatBytes(usedBytes), Modifier.weight(1.35f))
                    HeroMetric("المتاح", formatBytes((quotaBytes - usedBytes).coerceAtLeast(0)), Modifier.weight(1f))
                    HeroMetric("المتصل", "$activeDevices/$totalDevices", Modifier.weight(0.8f))
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.62f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeviceOrbitCard(
    device: ManagedDevice,
    onTogglePause: (ManagedDevice) -> Unit,
    onAddBonus: (ManagedDevice) -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when {
            device.paused || device.exhausted -> Danger
            device.progress >= 0.8f -> Amber
            else -> SignalBlue
        },
        label = "device-status"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuotaOrbit(device, statusColor)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(device.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        StatusPill(
                            text = if (device.paused || device.exhausted) "متوقف" else "يعمل",
                            color = statusColor
                        )
                    }
                    Text(
                        "${device.owner}  •  ${device.ipAddress}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        InlineMetric(Icons.Rounded.Speed, formatSpeed(device.downloadBps))
                        InlineMetric(Icons.Rounded.Add, "متبقي ${formatBytes(device.remainingBytes)}")
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onTogglePause(device) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.paused || device.exhausted) Success else Danger
                    )
                ) {
                    Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(7.dp))
                    Text(if (device.paused || device.exhausted) "فتح الإنترنت" else "إيقاف الآن")
                }
                FilledTonalButton(onClick = { onAddBonus(device) }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("500 MB")
                }
            }
        }
    }
}

@Composable
private fun QuotaOrbit(device: ManagedDevice, color: Color) {
    val progress by animateFloatAsState(device.progress, label = "quota-progress")
    Box(modifier = Modifier.size(82.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = color.copy(alpha = 0.14f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Icon(deviceIcon(device.kind), contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun DevicesScreen(
    devices: List<ManagedDevice>,
    onTogglePause: (ManagedDevice) -> Unit,
    onAddBonus: (ManagedDevice) -> Unit,
    onSetQuota: (ManagedDevice, Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeading("كل الأجهزة", "${devices.count { it.online }} متصل من ${devices.size}") }
        items(devices, key = { it.id }) { device ->
            DeviceDetailCard(device, onTogglePause, onAddBonus, onSetQuota)
        }
    }
}

@Composable
private fun DeviceDetailCard(
    device: ManagedDevice,
    onTogglePause: (ManagedDevice) -> Unit,
    onAddBonus: (ManagedDevice) -> Unit,
    onSetQuota: (ManagedDevice, Long) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(deviceIcon(device.kind), null, tint = SignalBlue)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleLarge)
                    Text(device.macAddress, style = MaterialTheme.typography.labelSmall, color = Muted)
                }
                Text("${(device.progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = if (device.progress > .8f) Amber else SignalBlue)
            }
            LinearProgressIndicator(
                progress = { device.progress },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = if (device.exhausted) Danger else SignalBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("استخدم ${formatBytes(device.usedBytes)}", color = Muted, style = MaterialTheme.typography.bodyMedium)
                Text("من ${formatBytes(device.quotaBytes)}", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSetQuota(device, 2L * 1024L * 1024L * 1024L) }, modifier = Modifier.weight(1f)) {
                    Text("حصة 2 GB")
                }
                OutlinedButton(onClick = { onAddBonus(device) }, modifier = Modifier.weight(1f)) {
                    Text("إضافة 500 MB")
                }
                IconButton(onClick = { onTogglePause(device) }) {
                    Icon(Icons.Rounded.PowerSettingsNew, "تبديل حالة الإنترنت", tint = if (device.paused) Success else Danger)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: AdminUiState,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onConnect: () -> Unit,
    onUseDemo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionHeading("ربط NetQuota Box", "الإدارة فقط تمر من هنا، والحصص تعمل داخل الجهاز") }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = state.connection.baseUrl,
                        onValueChange = onUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عنوان الـGateway") },
                        placeholder = { Text("192.168.50.1:8787") },
                        leadingIcon = { Icon(Icons.Rounded.Router, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    OutlinedTextField(
                        value = state.connection.token,
                        onValueChange = onTokenChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مفتاح الإدارة") },
                        leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.isRefreshing && state.connection.baseUrl.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null)
                        Spacer(Modifier.size(8.dp))
                        Text("حفظ واختبار الاتصال")
                    }
                    TextButton(onClick = onUseDemo, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("العودة إلى الوضع التجريبي")
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Home, null, tint = SignalBlue)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("تشغيل محلي أولًا", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "حتى عند غلق هذا التطبيق أو انقطاع السحابة، يحتفظ NetQuota Box بالحصص ويطبقها.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier.background(color.copy(alpha = 0.16f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall
    )
}

@Composable
private fun InlineMetric(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(15.dp))
        Spacer(Modifier.size(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Danger.copy(alpha = 0.12f)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = Danger)
        Spacer(Modifier.size(9.dp))
        Text(message, modifier = Modifier.weight(1f), color = Danger, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onDismiss) { Text("إغلاق", color = Danger) }
    }
}

private fun deviceIcon(kind: String): ImageVector = when (kind.lowercase()) {
    "phone" -> Icons.Rounded.PhoneAndroid
    "tv" -> Icons.Rounded.Tv
    "laptop", "computer" -> Icons.Rounded.Laptop
    else -> Icons.Rounded.DevicesOther
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (gb >= 1.0) String.format(Locale.US, "%.2f GB", gb) else String.format(Locale.US, "%.0f MB", mb)
}

private fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return "0 KB/s"
    val mb = bytesPerSecond.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format(Locale.US, "%.1f MB/s", mb) else String.format(Locale.US, "%.0f KB/s", bytesPerSecond / 1024.0)
}
