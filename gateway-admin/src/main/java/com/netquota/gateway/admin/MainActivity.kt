package com.netquota.gateway.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netquota.gateway.admin.ui.AdminApp
import com.netquota.gateway.admin.ui.AdminViewModel
import com.netquota.gateway.admin.ui.theme.NetQuotaGatewayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetQuotaGatewayTheme {
                val adminViewModel: AdminViewModel = viewModel()
                AdminApp(adminViewModel)
            }
        }
    }
}
