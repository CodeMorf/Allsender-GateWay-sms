package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GatewayRepository
import com.example.data.SMSLog
import com.example.service.GatewayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface

sealed class QRConnectionStatus {
    object Idle : QRConnectionStatus()
    data class Connecting(val message: String) : QRConnectionStatus()
    data class Connected(val gatewayName: String) : QRConnectionStatus()
    data class Error(val error: String) : QRConnectionStatus()
}

class GatewayViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GatewayRepository(application)
    private val context = application.applicationContext

    // Log Flows
    val logs: StateFlow<List<SMSLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sentCount: StateFlow<Int> = repository.sentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = repository.failedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Configuration / Setting states
    private val _port = MutableStateFlow(repository.getPort())
    val port = _port.asStateFlow()

    private val _apiKey = MutableStateFlow(repository.getApiKey())
    val apiKey = _apiKey.asStateFlow()

    // Real Allsender URL
    private val _baseUrl = MutableStateFlow(repository.getBaseUrl())
    val baseUrl = _baseUrl.asStateFlow()

    // Real Allsender Device Token
    private val _deviceToken = MutableStateFlow(repository.getDeviceToken())
    val deviceToken = _deviceToken.asStateFlow()

    private val _pollingUrl = MutableStateFlow(repository.getPollingUrl())
    val pollingUrl = _pollingUrl.asStateFlow()

    private val _pollingToken = MutableStateFlow(repository.getPollingToken())
    val pollingToken = _pollingToken.asStateFlow()

    private val _pollingEnabled = MutableStateFlow(repository.isPollingEnabled())
    val pollingEnabled = _pollingEnabled.asStateFlow()

    private val _simulationMode = MutableStateFlow(repository.isSimulationMode())
    val simulationMode = _simulationMode.asStateFlow()

    private val _sendEnabled = MutableStateFlow(repository.isSendEnabled())
    val sendEnabled = _sendEnabled.asStateFlow()

    private val _receiveEnabled = MutableStateFlow(repository.isReceiveEnabled())
    val receiveEnabled = _receiveEnabled.asStateFlow()

    private val _marketingEnabled = MutableStateFlow(repository.isMarketingEnabled())
    val marketingEnabled = _marketingEnabled.asStateFlow()

    private val _defaultSimSlot = MutableStateFlow(repository.getDefaultSimSlot())
    val defaultSimSlot = _defaultSimSlot.asStateFlow()

    private val _pollingInterval = MutableStateFlow(repository.getPollingInterval())
    val pollingInterval = _pollingInterval.asStateFlow()

    private val _forwardInbound = MutableStateFlow(repository.isForwardInbound())
    val forwardInbound = _forwardInbound.asStateFlow()

    private val _forwardUrl = MutableStateFlow(repository.getForwardUrl())
    val forwardUrl = _forwardUrl.asStateFlow()

    private val _isServerActive = MutableStateFlow(repository.isServerActive())
    val isServerActive = _isServerActive.asStateFlow()

    private val _localIp = MutableStateFlow("127.0.0.1")
    val localIp = _localIp.asStateFlow()

    private val _qrConnectionState = MutableStateFlow<QRConnectionStatus>(QRConnectionStatus.Idle)
    val qrConnectionState = _qrConnectionState.asStateFlow()

    private val _gatewayName = MutableStateFlow(repository.getGatewayName())
    val gatewayName = _gatewayName.asStateFlow()

    private val _gatewayId = MutableStateFlow(repository.getGatewayId())
    val gatewayId = _gatewayId.asStateFlow()

    // SIM Cards flow
    private val _activeSims = MutableStateFlow<List<GatewayRepository.SimCardInfo>>(emptyList())
    val activeSims = _activeSims.asStateFlow()

    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "pref_server_active") {
            _isServerActive.value = repository.isServerActive()
        } else if (key == "pref_manual_provider") {
            _manualProvider.value = repository.getManualProvider()
        }
    }

    init {
        repository.registerPreferenceChangeListener(preferenceListener)
        updateLocalIp()
        updateActiveSims()
    }

    override fun onCleared() {
        super.onCleared()
        repository.unregisterPreferenceChangeListener(preferenceListener)
    }

    fun updateActiveSims() {
        _activeSims.value = repository.getActiveSimCards(context)
    }

    fun updateLocalIp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                var foundIp = "127.0.0.1"
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val inetAddress = addresses.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            foundIp = inetAddress.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
                _localIp.value = foundIp
            } catch (e: Exception) {
                Log.e("GatewayViewModel", "Error reading local IP: ${e.message}")
            }
        }
    }

    // Config actions
    fun savePort(value: Int) {
        repository.setPort(value)
        _port.value = value
    }

    fun saveApiKey(value: String) {
        repository.setApiKey(value)
        _apiKey.value = value
    }

    fun saveBaseUrl(value: String) {
        repository.setBaseUrl(value)
        _baseUrl.value = value
        _pollingUrl.value = repository.getPollingUrl()
    }

    fun saveDeviceToken(value: String) {
        repository.setDeviceToken(value)
        _deviceToken.value = value
        _pollingToken.value = value
    }

    fun savePollingUrl(value: String) {
        repository.setBaseUrl(value)
        _pollingUrl.value = value
    }

    fun savePollingToken(value: String) {
        repository.setDeviceToken(value)
        _pollingToken.value = value
        _deviceToken.value = value
    }

    fun savePollingEnabled(value: Boolean) {
        repository.setPollingEnabled(value)
        _pollingEnabled.value = value
    }

    fun saveSimulationMode(value: Boolean) {
        repository.setSimulationMode(value)
        _simulationMode.value = value
    }

    fun saveSendEnabled(value: Boolean) {
        repository.setSendEnabled(value)
        _sendEnabled.value = value
    }

    fun saveReceiveEnabled(value: Boolean) {
        repository.setReceiveEnabled(value)
        _receiveEnabled.value = value
        _forwardInbound.value = value
    }

    fun saveMarketingEnabled(value: Boolean) {
        repository.setMarketingEnabled(value)
        _marketingEnabled.value = value
    }

    fun saveDefaultSimSlot(value: Int) {
        repository.setDefaultSimSlot(value)
        _defaultSimSlot.value = value
    }

    fun savePollingInterval(value: Int) {
        repository.setPollingInterval(value)
        _pollingInterval.value = value
    }

    fun saveForwardInbound(value: Boolean) {
        saveReceiveEnabled(value)
    }

    fun saveForwardUrl(value: String) {
        repository.setBaseUrl(value)
        _forwardUrl.value = value
    }

    private val _manualProvider = MutableStateFlow(repository.getManualProvider())
    val manualProvider = _manualProvider.asStateFlow()

    fun saveManualProvider(value: String) {
        repository.setManualProvider(value)
        _manualProvider.value = value
    }

    fun getLastHeartbeat(): String = repository.getLastHeartbeat()
    fun getLastPolling(): String = repository.getLastPolling()
    fun getLastSmsReceived(): String = repository.getLastSmsReceived()
    fun getLastSmsSent(): String = repository.getLastSmsSent()
    fun getApiError(): String = repository.getApiError()
    fun getAndroidError(): String = repository.getAndroidError()
    fun getServerEvents(): String = repository.getServerEvents()
    
    fun clearDiagnostics() {
        repository.clearDiagnostics()
    }

    // Action Methods

    fun activateServerMode(intervalSeconds: Int = 10) {
        repository.setPollingEnabled(true)
        repository.setPollingInterval(intervalSeconds.coerceIn(5, 60))
        repository.setSendEnabled(true)
        repository.setReceiveEnabled(true)
        repository.setMarketingEnabled(true)
        repository.setServerModeEnabled(true)
        repository.appendServerEvent("BOTÓN ACTIVAR: iniciando modo servidor cada ${intervalSeconds.coerceIn(5, 60)}s")

        _pollingEnabled.value = true
        _pollingInterval.value = intervalSeconds.coerceIn(5, 60)
        _sendEnabled.value = true
        _receiveEnabled.value = true
        _marketingEnabled.value = true

        startService()

        viewModelScope.launch {
            val success = repository.sendHeartbeat()
            repository.appendServerEvent(if (success) "BOTÓN ACTIVAR: conectado y sincronizado" else "BOTÓN ACTIVAR: no conectó con Allsender")
        }
    }

    fun toggleGatewayServer() {
        if (_isServerActive.value) {
            stopService()
        } else {
            startService()
        }
    }

    fun startService() {
        val hasSmsPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasPhonePerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasSmsPerm || !hasPhonePerm) {
            val errorMsg = "Fallo al iniciar servicio: Permisos de SMS o Lectura de Teléfono ausentes"
            Log.e("GatewayViewModel", errorMsg)
            repository.saveAndroidError(errorMsg)
            return
        }

        try {
            val intent = Intent(context, GatewayService::class.java).apply {
                action = GatewayService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
            Log.d("GatewayViewModel", "Comando startForegroundService enviado correctamente")
        } catch (e: Exception) {
            val errorMsg = "Error try-catch startForegroundService: ${e.message}"
            Log.e("GatewayViewModel", errorMsg, e)
            repository.saveAndroidError(errorMsg)
        }
    }

    fun stopService() {
        repository.setServerModeEnabled(false)
        repository.appendServerEvent("SERVIDOR APAGADO: detenido por el usuario")
        try {
            val intent = Intent(context, GatewayService::class.java).apply {
                action = GatewayService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("GatewayViewModel", "Error al detener servicio: ${e.message}", e)
        }
    }

    fun forcePollMessages() {
        viewModelScope.launch {
            repository.pollPendingSMS()
        }
    }

    fun forceHeartbeat(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.sendHeartbeat()
            onResult(success)
        }
    }

    fun sendTestSMS(to: String, text: String, simSlot: Int = 1) {
        viewModelScope.launch {
            repository.sendSMS(to, text, "MANUAL", simSlot)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLogById(id)
        }
    }

    fun resetQRConnectionState() {
        _qrConnectionState.value = QRConnectionStatus.Idle
    }

    fun connectWithQRCode(jsonString: String) {
        viewModelScope.launch {
            _qrConnectionState.value = QRConnectionStatus.Connecting("Validando QR...")
            try {
                val textTrimmed = jsonString.trim()
                if (textTrimmed.startsWith("{")) {
                    val json = JSONObject(textTrimmed)
                    val type = json.optString("type")
                    if (type != "allsender_sms_gateway_connect") {
                        // Check if it's a generic JSON containing device_token or token
                        val possibleToken = json.optString("device_token", json.optString("token", ""))
                        if (possibleToken.isNotBlank()) {
                            connectWithRawToken(possibleToken)
                            return@launch
                        }
                        _qrConnectionState.value = QRConnectionStatus.Error("Código QR no válido para Allsender Gateway.")
                        return@launch
                    }

                    val version = json.optInt("version", 1)
                    val baseUrlVal = json.optString("base_url")
                    val deviceTokenVal = json.optString("device_token")
                    val gatewayIdVal = json.optInt("gateway_id", 0)
                    val gatewayNameVal = json.optString("name")
                    val pollingUrlVal = json.optString("polling_url")
                    val statusUrlVal = json.optString("status_url")
                    val heartbeatUrlVal = json.optString("heartbeat_url")
                    val inboundUrlVal = json.optString("inbound_url")

                    _qrConnectionState.value = QRConnectionStatus.Connecting("Conectando con ${gatewayNameVal ?: "Servidor"}...")

                    // Temporarily save to perform test heartbeat
                    val originBase = repository.getBaseUrl()
                    val originToken = repository.getDeviceToken()
                    val originName = repository.getGatewayName()
                    val originId = repository.getGatewayId()
                    val originPoll = repository.getCustomPollingUrl()
                    val originStatus = repository.getCustomStatusUrl()
                    val originHeartbeat = repository.getCustomHeartbeatUrl()
                    val originInbound = repository.getCustomInboundUrl()

                    repository.setBaseUrl(baseUrlVal)
                    repository.setDeviceToken(deviceTokenVal)
                    repository.setGatewayId(gatewayIdVal)
                    repository.setGatewayName(gatewayNameVal)
                    repository.setCustomPollingUrl(pollingUrlVal)
                    repository.setCustomStatusUrl(statusUrlVal)
                    repository.setCustomHeartbeatUrl(heartbeatUrlVal)
                    repository.setCustomInboundUrl(inboundUrlVal)

                    // Execute test heartbeat
                    val ok = repository.sendHeartbeat()
                    if (ok) {
                        _baseUrl.value = baseUrlVal
                        _deviceToken.value = deviceTokenVal
                        _pollingUrl.value = repository.getPollingUrl()
                        _pollingToken.value = deviceTokenVal
                        _forwardUrl.value = repository.getForwardUrl()
                        _gatewayName.value = gatewayNameVal
                        _gatewayId.value = gatewayIdVal
                        _qrConnectionState.value = QRConnectionStatus.Connected(gatewayNameVal)
                        
                        // Activate Automatic polling and start background services
                        repository.setPollingEnabled(true)
                        _pollingEnabled.value = true
                        startService()
                    } else {
                        // Rollback if heartbeat fails
                        repository.setBaseUrl(originBase)
                        repository.setDeviceToken(originToken)
                        repository.setGatewayId(originId)
                        repository.setGatewayName(originName)
                        repository.setCustomPollingUrl(originPoll)
                        repository.setCustomStatusUrl(originStatus)
                        repository.setCustomHeartbeatUrl(originHeartbeat)
                        repository.setCustomInboundUrl(originInbound)

                        _qrConnectionState.value = QRConnectionStatus.Error("Error al validar enlace (Heartbeat fallido). Verifique la conexión a internet.")
                    }
                } else {
                    // Treat as raw token
                    if (textTrimmed.isNotBlank()) {
                        connectWithRawToken(textTrimmed)
                    } else {
                        _qrConnectionState.value = QRConnectionStatus.Error("Código QR vacío.")
                    }
                }
            } catch (e: Exception) {
                Log.e("GatewayViewModel", "Error parsing QR JSON: ${e.message}")
                val textTrimmed = jsonString.trim()
                if (textTrimmed.isNotBlank() && textTrimmed.length > 5) {
                    connectWithRawToken(textTrimmed)
                } else {
                    _qrConnectionState.value = QRConnectionStatus.Error("Formato de QR no válido.")
                }
            }
        }
    }

    private suspend fun connectWithRawToken(token: String) {
        _qrConnectionState.value = QRConnectionStatus.Connecting("Validando Token...")
        
        val originBase = repository.getBaseUrl()
        val originToken = repository.getDeviceToken()
        val originName = repository.getGatewayName()
        val originId = repository.getGatewayId()

        val currentBase = if (repository.getBaseUrl().isBlank()) "https://auth.allsender.tech" else repository.getBaseUrl()
        repository.setBaseUrl(currentBase)
        repository.setDeviceToken(token)
        repository.setGatewayName("Dispositivo Enlazado")
        repository.setGatewayId(1)

        val ok = repository.sendHeartbeat()
        if (ok) {
            _baseUrl.value = currentBase
            _deviceToken.value = token
            _pollingUrl.value = repository.getPollingUrl()
            _pollingToken.value = token
            _forwardUrl.value = repository.getForwardUrl()
            _gatewayName.value = "Dispositivo Enlazado"
            _gatewayId.value = 1
            _qrConnectionState.value = QRConnectionStatus.Connected("Dispositivo Enlazado")
            
            repository.setPollingEnabled(true)
            _pollingEnabled.value = true
            startService()
        } else {
            repository.setBaseUrl(originBase)
            repository.setDeviceToken(originToken)
            repository.setGatewayName(originName)
            repository.setGatewayId(originId)
            _qrConnectionState.value = QRConnectionStatus.Error("Error al validar Token (Heartbeat fallido). Verifique el token o la conexión.")
        }
    }

    fun disconnectGateway() {
        repository.setDeviceToken("")
        repository.setGatewayId(0)
        repository.setGatewayName("")
        repository.setCustomPollingUrl("")
        repository.setCustomStatusUrl("")
        repository.setCustomHeartbeatUrl("")
        repository.setCustomInboundUrl("")
        
        _baseUrl.value = "https://auth.allsender.tech"
        repository.setBaseUrl("https://auth.allsender.tech")
        _deviceToken.value = ""
        _pollingToken.value = ""
        _gatewayName.value = ""
        _gatewayId.value = 0
        _forwardUrl.value = repository.getForwardUrl()
        _pollingUrl.value = repository.getPollingUrl()
        
        // Also stop active service automatically on disconnect
        stopService()
        repository.setPollingEnabled(false)
        _pollingEnabled.value = false
        
        _qrConnectionState.value = QRConnectionStatus.Idle
    }
}
