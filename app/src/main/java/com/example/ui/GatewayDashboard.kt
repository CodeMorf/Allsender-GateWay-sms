package com.example.ui

import android.Manifest
import android.content.Context
import android.provider.Settings
import android.os.PowerManager
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.SMSLog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Brand Visual Theme Constants
val ThemePrimaryGold = Color(0xFFC19754)
val ThemeDarkBlue = Color(0xFF111E2E)
val ThemeBgCream = Color(0xFFFAF8F5)
val ThemeCardBorder = Color(0xFFEADFC9)
val ThemeSuccessGreen = Color(0xFF10B981)
val ThemeSoftSuccessBg = Color(0xFFE6F4EA)
val ThemeSoftSuccessBorder = Color(0xFFC3E6CB)

@Composable
fun HexagonLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(54.dp)
            .background(Color(0xFFF1EDE4), CircleShape)
            .border(1.5.dp, ThemePrimaryGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ThemePrimaryGold, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PulsingStatusDot(color: Color = ThemeSuccessGreen) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(800)
            visible = !visible
        }
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (visible) color else color.copy(alpha = 0.30f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayDashboard(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Permission status checks
    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermissions = results[Manifest.permission.SEND_SMS] == true &&
                            results[Manifest.permission.RECEIVE_SMS] == true
        if (hasSmsPermissions) {
            Toast.makeText(context, "Permisos concedidos con éxito", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permisos de SMS rechazados. El envío real puede fallar.", Toast.LENGTH_LONG).show()
        }
        viewModel.updateActiveSims()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimaryGold,
                        selectedTextColor = ThemePrimaryGold,
                        indicatorColor = Color(0xFFFAF2E6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Actividad") },
                    label = { Text("Actividad") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimaryGold,
                        selectedTextColor = ThemePrimaryGold,
                        indicatorColor = Color(0xFFFAF2E6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimaryGold,
                        selectedTextColor = ThemePrimaryGold,
                        indicatorColor = Color(0xFFFAF2E6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Diagnóstico") },
                    label = { Text("Diag") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimaryGold,
                        selectedTextColor = ThemePrimaryGold,
                        indicatorColor = Color(0xFFFAF2E6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        },
        containerColor = ThemeBgCream
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> PanelScreen(
                    viewModel = viewModel,
                    hasSmsPermissions = hasSmsPermissions,
                    onRequestPermissions = {
                        val permissions = mutableListOf(
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_PHONE_STATE
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add("android.permission.POST_NOTIFICATIONS")
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    },
                    onNavigateToSettings = { selectedTab = 2 },
                    onNavigateToLogs = { selectedTab = 1 }
                )
                1 -> MessageLogsScreen(viewModel = viewModel)
                2 -> SettingsScreen(
                    viewModel = viewModel,
                    hasSmsPermissions = hasSmsPermissions,
                    onRequestPermissions = {
                        val permissions = mutableListOf(
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_PHONE_STATE
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add("android.permission.POST_NOTIFICATIONS")
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                )
                3 -> DiagnosticsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PanelScreen(
    viewModel: GatewayViewModel,
    hasSmsPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val context = LocalContext.current
    var showQRScanner by remember { mutableStateOf(false) }

    // Live States collected from ViewModel
    val isServerActive by viewModel.isServerActive.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val port by viewModel.port.collectAsState()
    val isSimulationMode by viewModel.simulationMode.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val deviceToken by viewModel.deviceToken.collectAsState()
    
    val countTotal by viewModel.totalCount.collectAsState()
    val countSent by viewModel.sentCount.collectAsState()
    val countFailed by viewModel.failedCount.collectAsState()
    val countPending by viewModel.pendingCount.collectAsState()
    val activeSims by viewModel.activeSims.collectAsState()
    val manualProvider by viewModel.manualProvider.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val connectedGatewayName by viewModel.gatewayName.collectAsState()
    val connectedGatewayId by viewModel.gatewayId.collectAsState()
    val defaultSimSlot by viewModel.defaultSimSlot.collectAsState()

    val isConfigured = connectedGatewayName.isNotBlank() || deviceToken.isNotBlank()
    var serverEvents by remember { mutableStateOf(viewModel.getServerEvents()) }

    // Connection timer counter
    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(isServerActive) {
        if (isServerActive) {
            elapsedSeconds = 0
            while (true) {
                kotlinx.coroutines.delay(1000)
                elapsedSeconds++
            }
        }
    }

    val activeUptimeString = remember(elapsedSeconds) {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        String.format("%02d:%02d:%02d", h, m, s)
    }

    LaunchedEffect(isServerActive) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            serverEvents = viewModel.getServerEvents()
        }
    }

    // Camera launcher for scanner
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showQRScanner = true
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para escanear el QR", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isConfigured) {
            // ================= STATE 1: UNCONNECTED WELCOME PORTAL =================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HexagonLogo()
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Allsender",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeDarkBlue,
                                    fontSize = 24.sp
                                )
                            )
                            Text(
                                text = "SMS Gateway",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = ThemePrimaryGold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, ThemeCardBorder, CircleShape)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes directos",
                            tint = ThemeDarkBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Bienvenido",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemeDarkBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Conecta tu teléfono con Allsender y envía y recibe SMS de forma profesional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Connection Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("connection_summary_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ThemeCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "RESUMEN DE CONEXIÓN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemePrimaryGold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estado", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF1EDE4))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Desconectado",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4A453A)
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = ThemeCardBorder.copy(alpha = 0.7f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Servidor", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                            Text(
                                text = if (baseUrl.isNotBlank()) baseUrl.replace("https://", "").replace("http://", "") else "auth.allsender.tech",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ThemeDarkBlue
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = ThemeCardBorder.copy(alpha = 0.7f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cuenta", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                            Text(
                                text = "No configurada",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            // Available SIMs Checklist
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ThemeCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SIM DISPONIBLES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemePrimaryGold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // SIM 1 Detection Row
                        val sim1 = activeSims.find { it.slot == 1 || it.slot == 0 }
                        val displaySim1Carrier = when {
                            manualProvider.isNotBlank() -> manualProvider
                            sim1 != null && sim1.carrierName.isNotBlank() && !sim1.carrierName.equals("Desconocido", ignoreCase = true) -> sim1.carrierName
                            else -> "Proveedor no detectado"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = ThemePrimaryGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = displaySim1Carrier,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = ThemeDarkBlue
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sim1 != null) ThemeSoftSuccessBg else Color(0xFFF1EDE4))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (sim1 != null) "Activa" else "Inactiva",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (sim1 != null) ThemeSuccessGreen else Color(0xFF4A453A)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = ThemeCardBorder.copy(alpha = 0.7f))

                        // SIM 2 Detection Row
                        val sim2 = activeSims.find { it.slot == 2 }
                        val displaySim2Carrier = when {
                            sim2 != null && sim2.carrierName.isNotBlank() && !sim2.carrierName.equals("Desconocido", ignoreCase = true) -> sim2.carrierName
                            sim2 != null -> "Proveedor no detectado"
                            else -> "SIM 2 (Opcional)"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = ThemePrimaryGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = displaySim2Carrier,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (sim2 != null) ThemeDarkBlue else Color(0xFF94A3B8)
                                    )
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sim2 != null) ThemeSoftSuccessBg else Color(0xFFF1EDE4))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (sim2 != null) "Activa" else "Inactiva",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (sim2 != null) ThemeSuccessGreen else Color(0xFF4A453A)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Unconnected Action CTA Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showQRScanner = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("scan_qr_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeDarkBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Escanear QR", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (!hasSmsPermissions) {
                                onRequestPermissions()
                            } else {
                                viewModel.activateServerMode(10)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("activate_gateway_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryGold)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isServerActive) "Servidor activo en segundo plano" else "Activar servidor en segundo plano",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

        } else {
            // ================= STATE 3: FULL ACTIVE CONNECTED DASHBOARD =================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot()
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gateway Activo",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeDarkBlue
                                )
                            )
                        }
                        Text(
                            text = "Tu gateway está conectado y operativo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(ThemeSoftSuccessBg)
                            .border(1.dp, ThemeSoftSuccessBorder, RoundedCornerShape(30.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "En línea",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThemeSuccessGreen
                        )
                    }
                }
            }

            // Green "Proveedor Detectado" Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("provider_detected_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeSoftSuccessBg),
                    border = BorderStroke(1.dp, ThemeSoftSuccessBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = ThemeSuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val firstSim = activeSims.firstOrNull()
                                val displayFirstSimCarrier = when {
                                    manualProvider.isNotBlank() -> manualProvider
                                    firstSim != null && firstSim.carrierName.isNotBlank() && !firstSim.carrierName.equals("Desconocido", ignoreCase = true) -> firstSim.carrierName
                                    else -> "Proveedor no detectado"
                                }
                                Text(
                                    text = displayFirstSimCarrier,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ThemeDarkBlue
                                )
                                Text(
                                    text = "Red móvil detectada • Señal fuerte",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "4G",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = ThemeSuccessGreen
                            )
                        }
                    }
                }
            }

            // Side-by-side SIM Detail Info Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SIM 1 Metric Container
                    val sim1 = activeSims.find { it.slot == 1 || it.slot == 0 }
                    Card(
                        modifier = Modifier.weight(1f).testTag("sim_card_1_panel"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, ThemeCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SIM 1", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ThemeDarkBlue)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(ThemeSoftSuccessBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Activa", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = ThemeSuccessGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sim1?.phoneNumber ?: "+34 600 111 222",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = ThemeSuccessGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Envío: $countSent", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = ThemeDarkBlue)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total: $countTotal", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = ThemeDarkBlue)
                            }
                        }
                    }

                    // SIM 2 Metric Container
                    val sim2 = activeSims.find { it.slot == 2 }
                    Card(
                        modifier = Modifier.weight(1f).testTag("sim_card_2_panel"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, ThemeCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SIM 2", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ThemeDarkBlue)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (sim2 != null) ThemeSoftSuccessBg else Color(0xFFF1EDE4))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (sim2 != null) "Activa" else "Inactiva",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = if (sim2 != null) ThemeSuccessGreen else Color(0xFF4A453A)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sim2?.phoneNumber ?: "+34 612 987 654",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = ThemeSuccessGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Envío: 0", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF94A3B8))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total: 0", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // Connection Status & Live Ticker
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ThemeCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFF1EDE4), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ThemePrimaryGold, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Estado de conexión", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = ThemeDarkBlue)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Conectado desde", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                            Text(
                                text = if (isServerActive) activeUptimeString else "00:00:00",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                color = ThemeDarkBlue
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = ThemeCardBorder.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Último heartbeat", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ThemeSuccessGreen))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hace 12 segundos", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = ThemeDarkBlue)
                            }
                        }
                    }
                }
            }


            // Terminal del servidor SMS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("server_terminal_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeDarkBlue),
                    border = BorderStroke(1.dp, Color(0xFF243044))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot(color = ThemeSuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Terminal del servidor",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Puedes salir de la pantalla o bloquear el teléfono: el servidor sigue activo en segundo plano solo si la notificación permanente está visible. Mantén internet, SIM activa y batería sin restricciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF07111F))
                                .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (serverEvents.isBlank()) "[--:--:--] Esperando sincronización..." else serverEvents,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
                                color = Color(0xFF86EFAC)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    viewModel.forceHeartbeat { success ->
                                        Toast.makeText(context, if (success) "Sincronización ejecutada" else "No conectó con Allsender", Toast.LENGTH_SHORT).show()
                                        serverEvents = viewModel.getServerEvents()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sincronizar ahora", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.stopService()
                                    serverEvents = viewModel.getServerEvents()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF64748B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Apagar servidor", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Actividad Reciente Feed Block
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Actividad reciente",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ThemeDarkBlue
                        )
                        Text(
                            text = "Ver todo",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ThemePrimaryGold),
                            modifier = Modifier.clickable { onNavigateToLogs() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (logs.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, ThemeCardBorder)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Ningún tráfico de SMS registrado todavía.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            logs.take(3).forEach { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, ThemeCardBorder.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            val isInbound = log.type == "INBOUND"
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isInbound) Color(0xFFE0F2FE) else ThemeSoftSuccessBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isInbound) Icons.Default.CheckCircle else Icons.Default.Send,
                                                    contentDescription = null,
                                                    tint = if (isInbound) Color(0xFF0284C7) else ThemeSuccessGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(log.phoneNumber, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThemeDarkBlue)
                                                Text(log.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B), maxLines = 1)
                                            }
                                        }

                                        val simpleTimeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                                        Text(
                                            text = simpleTimeFormat.format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Collapsible advanced tests form at the bottom
            item {
                var isExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("advanced_dev_collapse_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ThemeCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = ThemePrimaryGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Pruebas de Envío & API", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = ThemeDarkBlue)
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Test Form Fields
                            var testNumber by remember { mutableStateOf("") }
                            var testMessage by remember { mutableStateOf("") }
                            
                            OutlinedTextField(
                                value = testNumber,
                                onValueChange = { testNumber = it },
                                label = { Text("Teléfono de Destino") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemePrimaryGold,
                                    focusedLabelColor = ThemePrimaryGold
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = testMessage,
                                onValueChange = { testMessage = it },
                                label = { Text("Mensaje de Prueba") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemePrimaryGold,
                                    focusedLabelColor = ThemePrimaryGold
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (testNumber.isBlank() || testMessage.isBlank()) {
                                        Toast.makeText(context, "Por favor completa ambos campos", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.sendTestSMS(to = testNumber, text = testMessage, simSlot = defaultSimSlot)
                                        Toast.makeText(context, "Orden manual de SMS puesta en cola", Toast.LENGTH_SHORT).show()
                                        testNumber = ""
                                        testMessage = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enviar SMS de Prueba", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Power off button when server is active
            item {
                Button(
                    onClick = { viewModel.activateServerMode(10) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEB), contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apagar Gateway Allsender", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    // Camera scanner modal overlay triggering
    if (showQRScanner) {
        val clipboard = LocalClipboardManager.current
        QRScannerDialog(
            onDismissRequest = { showQRScanner = false },
            onQrCodeScanned = { rawJsonStr ->
                showQRScanner = false
                viewModel.connectWithQRCode(rawJsonStr)
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ThemeCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black
                ),
                color = color
            )
        }
    }
}

@Composable
fun MessageLogsScreen(viewModel: GatewayViewModel) {
    val logs by viewModel.logs.collectAsState()
    val timeFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONEXIÓN & ACTIVIDAD DE SMS (${logs.size})",
                style = MaterialTheme.typography.titleMedium,
                color = ThemeDarkBlue,
                fontWeight = FontWeight.Bold
            )
            
            if (logs.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearAllLogs() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Limpiar Todo",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFFC19754).copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Sin registros aún",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemeDarkBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Cualquier mensaje enviado o recibido en el gateway aparecerá en esta lista.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(260.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItem(log = log, timeFormat = timeFormat, onDelete = { viewModel.deleteLog(log.id) })
                }
            }
        }
    }
}

@Composable
fun LogItem(log: SMSLog, timeFormat: SimpleDateFormat, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("log_item_card_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ThemeCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isOutbound = log.type == "OUTBOUND"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isOutbound) Color(0xFFEFF6FF) else ThemeSoftSuccessBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOutbound) "SALIDA" else "ENTRADA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isOutbound) Color(0xFF2563EB) else ThemeSuccessGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = log.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemeDarkBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormat.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Borrar",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDelete() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (log.status) {
                        "SENT" -> ThemeSuccessGreen
                        "FAILED" -> Color(0xFFEF4444)
                        else -> Color(0xFFEAB308)
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (log.status) {
                            "SENT" -> "Enviado con éxito"
                            "FAILED" -> "Envío fallido"
                            else -> "Pendiente"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }

                Text(
                    text = "Vía: ${log.gatewayMode}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF64748B)
                )
            }

            if (log.status == "FAILED" && !log.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFEF2F2))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "Razón: ${log.errorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF991B1B)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: GatewayViewModel,
    hasSmsPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    var isTokenOnlyScan by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }

    // State collections
    val isServerActive by viewModel.isServerActive.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val port by viewModel.port.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    
    val baseUrl by viewModel.baseUrl.collectAsState()
    val deviceToken by viewModel.deviceToken.collectAsState()
    val pollingInterval by viewModel.pollingInterval.collectAsState()
    
    val sendEnabled by viewModel.sendEnabled.collectAsState()
    val receiveEnabled by viewModel.receiveEnabled.collectAsState()
    val marketingEnabled by viewModel.marketingEnabled.collectAsState()
    val defaultSimSlot by viewModel.defaultSimSlot.collectAsState()
    val isSimulationMode by viewModel.simulationMode.collectAsState()

    var showAdvancedConfig by remember { mutableStateOf(true) }

    // Forms Inputs
    var baseUrlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
    var deviceTokenInput by remember(deviceToken) { mutableStateOf(deviceToken) }
    var pollingIntervalInput by remember(pollingInterval) { mutableStateOf(pollingInterval.toString()) }
    var portInput by remember(port) { mutableStateOf(port.toString()) }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showQRScanner = true
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara paso escanear", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App title header of Settings
        item {
            Text(
                "CONFIGURACIÓN DE TERMINAL",
                style = MaterialTheme.typography.titleMedium,
                color = ThemeDarkBlue,
                fontWeight = FontWeight.Bold
            )
        }

        // Live connection card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DIRECCIÓN LOCAL DEL TERMINAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ThemePrimaryGold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isServerActive) ThemeSoftSuccessBg else Color(0xFFF1EDE4))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isServerActive) "Servidor Activo" else "Servidor Apagado",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = if (isServerActive) ThemeSuccessGreen else Color(0xFF4A453A)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "http://$localIp:$port",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = ThemeDarkBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Puedes usar esta dirección local para enviar comandos POST/GET HTTP directos de prueba desde tu red de oficina.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // QR Code automatic scanner shortcut
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeDarkBlue),
                border = BorderStroke(1.dp, ThemeDarkBlue)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isTokenOnlyScan = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showQRScanner = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = ThemePrimaryGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Enlace automático mediante QR", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Alinea la cámara web y configura todo al instante.", color = Color(0xFFCBCFDC), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                }
            }
        }

        // Active Simulation Preference Card Toggle
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simular Tráfico de SMS (Modo Sandbox)", color = ThemeDarkBlue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Ideal para probar la aplicación en emuladores o terminales sin chip SIM físico.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isSimulationMode,
                        onCheckedChange = { viewModel.saveSimulationMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ThemeSuccessGreen, checkedTrackColor = ThemeSoftSuccessBg)
                    )
                }
            }
        }

        // Expanded advanced configuration block
        if (showAdvancedConfig) {
            item {
                Card(
                    modifier = Modifier.testTag("device_credentials_form_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ThemeCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "CREDENCIALES DE DISPOSITIVO ALLSENDER",
                            style = MaterialTheme.typography.titleSmall,
                            color = ThemePrimaryGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = baseUrlInput,
                            onValueChange = { baseUrlInput = it },
                            label = { Text("URL de Servidor Base") },
                            modifier = Modifier.fillMaxWidth().testTag("base_url_input_field"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemePrimaryGold,
                                focusedLabelColor = ThemePrimaryGold
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = deviceTokenInput,
                            onValueChange = { deviceTokenInput = it },
                            label = { Text("Token de Dispositivo (device_token)") },
                            modifier = Modifier.fillMaxWidth().testTag("device_token_input"),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        isTokenOnlyScan = true
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            showQRScanner = true
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Escanear Token QR",
                                        tint = ThemePrimaryGold
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemePrimaryGold,
                                focusedLabelColor = ThemePrimaryGold
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pollingIntervalInput,
                            onValueChange = { pollingIntervalInput = it },
                            label = { Text("Frecuencia de consulta (Segundos)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemePrimaryGold,
                                focusedLabelColor = ThemePrimaryGold
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val intervalVal = pollingIntervalInput.toIntOrNull() ?: 15
                                if (intervalVal < 5) {
                                    Toast.makeText(context, "El intervalo no puede ser menor a 5 segundos por seguridad", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveBaseUrl(baseUrlInput)
                                    viewModel.saveDeviceToken(deviceTokenInput)
                                    viewModel.savePollingInterval(intervalVal)
                                    Toast.makeText(context, "Conexión Allsender guardada con éxito", Toast.LENGTH_SHORT).show()

                                    if (isServerActive) {
                                        viewModel.stopService()
                                        viewModel.startService()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("apply_settings_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Guardar y Enlazar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Traffic Preferences Switches Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PREFERENCIAS DE TRÁFICO",
                        style = MaterialTheme.typography.titleSmall,
                        color = ThemePrimaryGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Envío Outbound
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Habilitar Envío (Outbound)", color = ThemeDarkBlue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Permite a Allsender enviar SMS salientes utilizando este terminal.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = sendEnabled,
                            onCheckedChange = { viewModel.saveSendEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ThemeSuccessGreen, checkedTrackColor = ThemeSoftSuccessBg)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = ThemeCardBorder.copy(alpha = 0.5f))

                    // Reenvío Inbound
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reenvío de Recibidos (Inbound)", color = ThemeDarkBlue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Captura de forma real los SMS entrantes de esta tarjeta SIM y los reenvía a Allsender.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = receiveEnabled,
                            onCheckedChange = { viewModel.saveReceiveEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ThemeSuccessGreen, checkedTrackColor = ThemeSoftSuccessBg)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = ThemeCardBorder.copy(alpha = 0.5f))

                    // Marketing Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permitir SMS de Marketing", color = ThemeDarkBlue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Habilita o ignora el tráfico promocional y publicitario en este terminal.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = marketingEnabled,
                            onCheckedChange = { viewModel.saveMarketingEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ThemeSuccessGreen, checkedTrackColor = ThemeSoftSuccessBg)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = ThemeCardBorder.copy(alpha = 0.5f))

                    // Default SIM selector
                    Column {
                        Text("SIM de Envío por Defecto", color = ThemeDarkBlue, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Elige qué tarjeta SIM usar por defecto si Allsender no especifica un canal requerido.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2).forEach { slot ->
                                val active = defaultSimSlot == slot
                                Button(
                                    onClick = { viewModel.saveDefaultSimSlot(slot) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) ThemePrimaryGold else Color(0xFFF1EDE4),
                                        contentColor = if (active) Color.White else Color(0xFF4A453A)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("SIM CHIP $slot", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local API Access Credentials Card Settings
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "SERVIDOR HTTP LOCAL (RECIBIR API)",
                        style = MaterialTheme.typography.titleSmall,
                        color = ThemePrimaryGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("Puerto local HTTP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePrimaryGold,
                            focusedLabelColor = ThemePrimaryGold
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key Privada de Acceso") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePrimaryGold,
                            focusedLabelColor = ThemePrimaryGold
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Button(
                        onClick = {
                            val p = portInput.toIntOrNull()
                            if (p == null || p <= 0 || p > 65535) {
                                Toast.makeText(context, "Puerto local inválido (1-65535)", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.savePort(p)
                                viewModel.saveApiKey(apiKeyInput)
                                Toast.makeText(context, "Configuraciones guardadas localmente", Toast.LENGTH_SHORT).show()
                                
                                if (isServerActive) {
                                    viewModel.stopService()
                                    viewModel.startService()
                                    Toast.makeText(context, "Servidor HTTP interno reiniciado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeDarkBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Guardar Servidor HTTP Local", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Settings nested scan overlay
    if (showQRScanner) {
        val clipboard = LocalClipboardManager.current
        QRScannerDialog(
            onDismissRequest = { showQRScanner = false },
            onQrCodeScanned = { rawJsonStr ->
                showQRScanner = false
                if (isTokenOnlyScan) {
                    // Update only token field
                    deviceTokenInput = rawJsonStr
                    Toast.makeText(context, "Token copiado del escaneo con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.connectWithQRCode(rawJsonStr)
                }
            }
        )
    }
}

@Composable
fun DiagnosticsScreen(viewModel: GatewayViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Manual provider state
    val manualProvider by viewModel.manualProvider.collectAsState()
    var manualProviderInput by remember(manualProvider) { mutableStateOf(manualProvider) }
    
    // Real SMS test parameters
    var targetPhone by remember { mutableStateOf("") }
    var testMessage by remember { mutableStateOf("Allsender SMS Gateway: Prueba real de transmisión.") }
    val defaultSimSlot by viewModel.defaultSimSlot.collectAsState()
    var selectedTestSimSlot by remember(defaultSimSlot) { mutableIntStateOf(defaultSimSlot) }
    
    // Live Diagnostics stats
    var lastHeartbeat by remember { mutableStateOf(viewModel.getLastHeartbeat()) }
    var lastPolling by remember { mutableStateOf(viewModel.getLastPolling()) }
    var lastSmsRec by remember { mutableStateOf(viewModel.getLastSmsReceived()) }
    var lastSmsSent by remember { mutableStateOf(viewModel.getLastSmsSent()) }
    var apiError by remember { mutableStateOf(viewModel.getApiError()) }
    var androidError by remember { mutableStateOf(viewModel.getAndroidError()) }
    
    val activeSims by viewModel.activeSims.collectAsState()
    val isServerActive by viewModel.isServerActive.collectAsState()
    var permissionRefreshTick by remember { mutableIntStateOf(0) }

    val sendSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    val receiveSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    val readPhoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    } else {
        true
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val batteryUnrestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }

    val allCriticalReady = sendSmsGranted && receiveSmsGranted && readPhoneGranted && notificationsGranted && batteryUnrestricted

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRefreshTick++
        Toast.makeText(context, "Permisos actualizados. Revisa el diagnóstico.", Toast.LENGTH_SHORT).show()
    }

    val openNotificationSettings = {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir ajustes de notificación", Toast.LENGTH_SHORT).show()
        }
    }

    val openBatterySettings = {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(context, "Abre Ajustes > Batería > Sin restricciones", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // Helper to refresh live diagnostics
    val refreshStats = {
        lastHeartbeat = viewModel.getLastHeartbeat()
        lastPolling = viewModel.getLastPolling()
        lastSmsRec = viewModel.getLastSmsReceived()
        lastSmsSent = viewModel.getLastSmsSent()
        apiError = viewModel.getApiError()
        androidError = viewModel.getAndroidError()
    }
    
    // Refresh stats once on screen enter
    LaunchedEffect(Unit, permissionRefreshTick) {
        permissionRefreshTick.toString()
        refreshStats()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DIAGNÓSTICO Y CONECTIVIDAD",
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeDarkBlue,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { refreshStats() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sincronizar", tint = ThemePrimaryGold)
                }
            }
        }
        
        // Card 0: Real Android system status
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (allCriticalReady) Color.White else Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, if (allCriticalReady) ThemeCardBorder else Color(0xFFF59E0B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "ESTADO REAL DEL SERVIDOR",
                                style = MaterialTheme.typography.titleSmall,
                                color = ThemePrimaryGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isServerActive) "Servicio marcado como activo" else "Servicio apagado o no confirmado",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isServerActive) ThemeSuccessGreen else Color(0xFFEF4444)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (allCriticalReady && isServerActive) ThemeSoftSuccessBg else Color(0xFFFEE2E2))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (allCriticalReady && isServerActive) "LISTO" else "REVISAR",
                                color = if (allCriticalReady && isServerActive) ThemeSuccessGreen else Color(0xFFDC2626),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SystemCheckRow("Enviar SMS", sendSmsGranted, "Permitido", "Falta SEND_SMS")
                    SystemCheckRow("Recibir SMS", receiveSmsGranted, "Permitido", "Falta RECEIVE_SMS")
                    SystemCheckRow("Leer SIM / teléfono", readPhoneGranted, "Permitido", "Falta READ_PHONE_STATE")
                    SystemCheckRow("Cámara QR", cameraGranted, "Permitida", "Falta CAMERA")
                    SystemCheckRow("Notificación permanente", notificationsGranted, "Permitida", "Falta activar notificaciones")
                    SystemCheckRow("Batería sin restricciones", batteryUnrestricted, "Sin restricciones", "Android puede dormir el servicio")

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Para segundo plano real debe existir una notificación permanente visible. Si no ves la notificación, toca activar abajo y revisa permisos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val permissions = mutableListOf(
                                Manifest.permission.SEND_SMS,
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.CAMERA
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(permissions.toTypedArray())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Activar / revisar permisos", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { openNotificationSettings() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ThemePrimaryGold)
                        ) {
                            Text("Notificación", color = ThemeDarkBlue, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { openBatterySettings() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ThemePrimaryGold)
                        ) {
                            Text("Batería", color = ThemeDarkBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.activateServerMode(10)
                            permissionRefreshTick++
                            Toast.makeText(context, "Servidor solicitado. Busca la notificación permanente.", Toast.LENGTH_LONG).show()
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1800)
                                refreshStats()
                                permissionRefreshTick++
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeDarkBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Activar servidor + notificación permanente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Provider Custom Settings Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PROVEEDOR / OPERADOR DE RED",
                        style = MaterialTheme.typography.titleSmall,
                        color = ThemePrimaryGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Show detected carriers for Sims
                    Text(
                        "Detección física de redes asignadas:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (activeSims.isEmpty()) {
                        Text("No se detectan tarjetas SIM físicas activas.", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                    } else {
                        activeSims.forEach { sim ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (sim.active) ThemeSuccessGreen else Color.Gray, CircleShape)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Slot ${sim.slot}: ${sim.carrierName} (${sim.countryIso.uppercase()})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThemeDarkBlue
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = ThemeCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        "Proveedor manual personalizado (Opcional):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = ThemeDarkBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Si el operador detectado por Android es incorrecto o vacío, ingresa el correcto aquí (ej: Claro, Viva, Altice, Movistar).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualProviderInput,
                        onValueChange = { manualProviderInput = it },
                        label = { Text("Nombre de Operador Manual") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_provider_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePrimaryGold,
                            focusedLabelColor = ThemePrimaryGold
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.saveManualProvider(manualProviderInput)
                            Toast.makeText(context, "Proveedor manual actualizado y guardado", Toast.LENGTH_SHORT).show()
                            refreshStats()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Guardar Proveedor Manual", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // Card 2: Diagnostics Stats and Errors
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "INFORME DE TELEMETRÍA",
                        style = MaterialTheme.typography.titleSmall,
                        color = ThemePrimaryGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DiagnosticField(label = "Último Heartbeat (Panel)", value = lastHeartbeat)
                    DiagnosticField(label = "Último Polling (Consulta)", value = lastPolling)
                    DiagnosticField(label = "Último SMS Recibido", value = lastSmsRec)
                    DiagnosticField(label = "Último SMS Enviado", value = lastSmsSent)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = ThemeCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // API Error text
                    Text("Detalles de Conexión API / Red:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = apiError.ifBlank { "Sin errores de red registrados." },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (apiError.isNotBlank()) Color.Red else ThemeSuccessGreen
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Android OS / Foreground Error text
                    Text("Incidencias Internas Android / SMS:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = androidError.ifBlank { "Sin incidencias operativas." },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (androidError.isNotBlank()) Color.Red else ThemeSuccessGreen
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            viewModel.clearDiagnostics()
                            Toast.makeText(context, "Historial de diagnóstico reiniciado", Toast.LENGTH_SHORT).show()
                            refreshStats()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeDarkBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Limpiar Registro de Errores", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // Card 3: Real manual Outbound sending test
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PRUEBA DE ENVÍO SMS INDEPENDIENTE",
                        style = MaterialTheme.typography.titleSmall,
                        color = ThemePrimaryGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Utiliza este módulo para validar la transmisión real del chip SIM por fuera de la cola de Allsender.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    OutlinedTextField(
                        value = targetPhone,
                        onValueChange = { targetPhone = it },
                        label = { Text("Teléfono destino (ej: +18095551234)") },
                        modifier = Modifier.fillMaxWidth().testTag("test_sms_phone_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePrimaryGold,
                            focusedLabelColor = ThemePrimaryGold
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = testMessage,
                        onValueChange = { testMessage = it },
                        label = { Text("Mensaje a transmitir") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePrimaryGold,
                            focusedLabelColor = ThemePrimaryGold
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // SIM Slot Selector for Testing
                    Text("Chip canal de transmisión física:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = ThemeDarkBlue)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2).forEach { slot ->
                            val active = selectedTestSimSlot == slot
                            Button(
                                onClick = { selectedTestSimSlot = slot },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) ThemePrimaryGold else Color(0xFFF1EDE4),
                                    contentColor = if (active) Color.White else Color(0xFF4A453A)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SIM $slot", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (targetPhone.isBlank()) {
                                Toast.makeText(context, "Ingresa un número de celular destino", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.sendTestSMS(targetPhone, testMessage, selectedTestSimSlot)
                                Toast.makeText(context, "Orden de transmisión encolada en SIM $selectedTestSimSlot", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    refreshStats()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("send_test_sms_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Iniciar Envío Manual Real", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // Card 4: Quick action tests
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ACCIONES RÁPIDAS DE CONEXIÓN",
                        style = MaterialTheme.typography.titleSmall,
                        color = ThemePrimaryGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            viewModel.forceHeartbeat { success ->
                                if (success) {
                                    Toast.makeText(context, "Heartbeat manual exitoso", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Fallo al conectar con el servidor", Toast.LENGTH_SHORT).show()
                                }
                                refreshStats()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeDarkBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Forzar sincronización heartbeat + cola", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF64748B))
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = ThemeDarkBlue)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = Color(0xFFF1EDE4))
    }
}


@Composable
fun SystemCheckRow(
    label: String,
    ok: Boolean,
    okText: String,
    badText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF475569)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(if (ok) ThemeSoftSuccessBg else Color(0xFFFEE2E2))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (ok) okText else badText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = if (ok) ThemeSuccessGreen else Color(0xFFDC2626)
            )
        }
    }
}
