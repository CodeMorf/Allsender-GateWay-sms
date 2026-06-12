package com.example.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class GatewayRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val smsLogDao = database.smsLogDao()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Flow listings
    val allLogs: Flow<List<SMSLog>> = smsLogDao.getAllSMSLogs()
    val totalCount: Flow<Int> = smsLogDao.getTotalSMSLogsCount()
    val sentCount: Flow<Int> = smsLogDao.getSMSLogsCountByStatus("SENT")
    val failedCount: Flow<Int> = smsLogDao.getSMSLogsCountByStatus("FAILED")
    val pendingCount: Flow<Int> = smsLogDao.getSMSLogsCountByStatus("PENDING")

    // Preference config helper keys
    companion object {
        private const val PREFS_NAME = "allsender_gateway_prefs"
        const val KEY_PORT = "pref_port"
        const val KEY_API_KEY = "pref_api_key"
        
        // Real Allsender fields
        const val KEY_BASE_URL = "pref_base_url"
        const val KEY_DEVICE_TOKEN = "pref_device_token"
        const val KEY_POLLING_ENABLED = "pref_polling_enabled"
        const val KEY_SIMULATION_MODE = "pref_simulation_mode"
        const val KEY_SEND_ENABLED = "pref_send_enabled"
        const val KEY_RECEIVE_ENABLED = "pref_receive_enabled"
        const val KEY_MARKETING_ENABLED = "pref_marketing_enabled"
        const val KEY_DEFAULT_SIM_SLOT = "pref_default_sim_slot"
        const val KEY_POLLING_INTERVAL = "pref_polling_interval"
        const val KEY_SERVER_ACTIVE = "pref_server_active"

        // QR Code Scanned custom data
        const val KEY_GATEWAY_ID = "pref_gateway_id"
        const val KEY_GATEWAY_NAME = "pref_gateway_name"
        const val KEY_POLLING_URL = "pref_polling_url"
        const val KEY_STATUS_URL = "pref_status_url"
        const val KEY_HEARTBEAT_URL = "pref_heartbeat_url"
        const val KEY_INBOUND_URL = "pref_inbound_url"

        // Telemetry diagnostics and manual operator preferences
        const val KEY_MANUAL_PROVIDER = "pref_manual_provider"
        const val KEY_LAST_HEARTBEAT_STATUS = "pref_last_heartbeat_status"
        const val KEY_LAST_HEARTBEAT_TIME = "pref_last_heartbeat_time"
        const val KEY_LAST_POLLING_STATUS = "pref_last_polling_status"
        const val KEY_LAST_POLLING_TIME = "pref_last_polling_time"
        const val KEY_LAST_SMS_RECEIVED = "pref_last_sms_received"
        const val KEY_LAST_SMS_SENT = "pref_last_sms_sent"
        const val KEY_LAST_API_ERROR = "pref_last_api_error"
        const val KEY_LAST_ANDROID_ERROR = "pref_last_android_error"
        const val KEY_SERVER_EVENTS = "pref_server_events"
        const val KEY_SERVER_MODE_ENABLED = "pref_server_mode_enabled"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun registerPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun getPort(): Int = prefs.getInt(KEY_PORT, 8080)
    fun setPort(port: Int) = prefs.edit().putInt(KEY_PORT, port).apply()

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "AllsenderSecretKey") ?: "AllsenderSecretKey"
    fun setApiKey(key: String) = prefs.edit().putString(KEY_API_KEY, key).apply()

    // Mapped methods
    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, "https://auth.allsender.tech") ?: "https://auth.allsender.tech"
    fun setBaseUrl(url: String) = prefs.edit().putString(KEY_BASE_URL, url).apply()

    fun getDeviceToken(): String = prefs.getString(KEY_DEVICE_TOKEN, "") ?: ""
    fun setDeviceToken(token: String) = prefs.edit().putString(KEY_DEVICE_TOKEN, token).apply()

    fun getGatewayId(): Int = prefs.getInt(KEY_GATEWAY_ID, 0)
    fun setGatewayId(value: Int) = prefs.edit().putInt(KEY_GATEWAY_ID, value).apply()

    fun getGatewayName(): String = prefs.getString(KEY_GATEWAY_NAME, "") ?: ""
    fun setGatewayName(value: String) = prefs.edit().putString(KEY_GATEWAY_NAME, value).apply()

    fun getCustomPollingUrl(): String = prefs.getString(KEY_POLLING_URL, "") ?: ""
    fun setCustomPollingUrl(value: String) = prefs.edit().putString(KEY_POLLING_URL, value).apply()

    fun getCustomStatusUrl(): String = prefs.getString(KEY_STATUS_URL, "") ?: ""
    fun setCustomStatusUrl(value: String) = prefs.edit().putString(KEY_STATUS_URL, value).apply()

    fun getCustomHeartbeatUrl(): String = prefs.getString(KEY_HEARTBEAT_URL, "") ?: ""
    fun setCustomHeartbeatUrl(value: String) = prefs.edit().putString(KEY_HEARTBEAT_URL, value).apply()

    fun getCustomInboundUrl(): String = prefs.getString(KEY_INBOUND_URL, "") ?: ""
    fun setCustomInboundUrl(value: String) = prefs.edit().putString(KEY_INBOUND_URL, value).apply()

    fun getPollingUrl(): String {
        val custom = getCustomPollingUrl()
        if (custom.isNotBlank()) return custom
        val base = getBaseUrl()
        return if (base.endsWith("/")) "${base}api/v1/sms/gateway" else "$base/api/v1/sms/gateway"
    }

    fun getPollingToken(): String = getDeviceToken()

    fun getForwardUrl(): String {
        val custom = getCustomInboundUrl()
        if (custom.isNotBlank()) return custom
        val base = getBaseUrl()
        return if (base.endsWith("/")) "${base}api/v1/sms/inbound" else "$base/api/v1/sms/inbound"
    }

    fun getHeartbeatUrl(): String {
        val custom = getCustomHeartbeatUrl()
        if (custom.isNotBlank()) return custom
        val base = getBaseUrl()
        return if (base.endsWith("/")) "${base}api/v1/sms/gateway/heartbeat" else "$base/api/v1/sms/gateway/heartbeat"
    }

    fun getStatusUrl(): String {
        val custom = getCustomStatusUrl()
        if (custom.isNotBlank()) return custom
        val base = getBaseUrl()
        return if (base.endsWith("/")) "${base}api/v1/sms/gateway/status" else "$base/api/v1/sms/gateway/status"
    }

    fun isPollingEnabled(): Boolean = prefs.getBoolean(KEY_POLLING_ENABLED, true)
    fun setPollingEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_POLLING_ENABLED, enabled).apply()

    fun isSimulationMode(): Boolean = prefs.getBoolean(KEY_SIMULATION_MODE, false) // Default to false for real usage!
    fun setSimulationMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_SIMULATION_MODE, enabled).apply()

    fun isSendEnabled(): Boolean = prefs.getBoolean(KEY_SEND_ENABLED, true)
    fun setSendEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SEND_ENABLED, enabled).apply()

    fun isReceiveEnabled(): Boolean = prefs.getBoolean(KEY_RECEIVE_ENABLED, true)
    fun setReceiveEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_RECEIVE_ENABLED, enabled).apply()

    fun isMarketingEnabled(): Boolean = prefs.getBoolean(KEY_MARKETING_ENABLED, true)
    fun setMarketingEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_MARKETING_ENABLED, enabled).apply()

    fun getDefaultSimSlot(): Int = prefs.getInt(KEY_DEFAULT_SIM_SLOT, 1)
    fun setDefaultSimSlot(slot: Int) = prefs.edit().putInt(KEY_DEFAULT_SIM_SLOT, slot).apply()

    fun getPollingInterval(): Int = prefs.getInt(KEY_POLLING_INTERVAL, 15)
    fun setPollingInterval(seconds: Int) = prefs.edit().putInt(KEY_POLLING_INTERVAL, seconds).apply()

    fun isServerActive(): Boolean = prefs.getBoolean(KEY_SERVER_ACTIVE, false)
    fun setServerActive(active: Boolean) = prefs.edit().putBoolean(KEY_SERVER_ACTIVE, active).apply()

    // Diagnostics telemetry getters & setters
    fun getManualProvider(): String = prefs.getString(KEY_MANUAL_PROVIDER, "") ?: ""
    fun setManualProvider(value: String) = prefs.edit().putString(KEY_MANUAL_PROVIDER, value).apply()

    fun getActiveProvider(context: Context, slot: Int): String {
        val manual = getManualProvider().trim()
        if (manual.isNotEmpty()) {
            return manual
        }
        val carrier = getCarrierNameForSlot(context, slot)
        if (carrier.isNotBlank() && !carrier.equals("Desconocido", ignoreCase = true)) {
            return carrier
        }
        return "Proveedor no detectado"
    }

    fun saveApiError(error: String) {
        prefs.edit().putString(KEY_LAST_API_ERROR, error).apply()
    }
    fun getApiError(): String = prefs.getString(KEY_LAST_API_ERROR, "") ?: ""

    fun saveAndroidError(error: String) {
        prefs.edit().putString(KEY_LAST_ANDROID_ERROR, error).apply()
    }
    fun getAndroidError(): String = prefs.getString(KEY_LAST_ANDROID_ERROR, "") ?: ""

    fun appendServerEvent(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] $message"
        val current = prefs.getString(KEY_SERVER_EVENTS, "") ?: ""
        val existingLines = current.lines().filter { it.isNotBlank() }
        val next = (listOf(line) + existingLines).take(14).joinToString("\n")
        prefs.edit().putString(KEY_SERVER_EVENTS, next).apply()
    }

    fun getServerEvents(): String = prefs.getString(KEY_SERVER_EVENTS, "") ?: ""

    
    fun setServerModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVER_MODE_ENABLED, enabled).apply()
    }

    fun isServerModeEnabled(): Boolean = prefs.getBoolean(KEY_SERVER_MODE_ENABLED, false)

    fun clearServerEvents() {
        prefs.edit().remove(KEY_SERVER_EVENTS).apply()
    }


    fun saveLastHeartbeatStatus(status: String) {
        prefs.edit().putString(KEY_LAST_HEARTBEAT_STATUS, status).apply()
        prefs.edit().putString(KEY_LAST_HEARTBEAT_TIME, SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())).apply()
    }
    fun getLastHeartbeat(): String {
        val status = prefs.getString(KEY_LAST_HEARTBEAT_STATUS, "Sin estado") ?: "Sin estado"
        val time = prefs.getString(KEY_LAST_HEARTBEAT_TIME, "") ?: ""
        return if (time.isNotEmpty()) "$status ($time)" else status
    }

    fun saveLastPollingStatus(status: String) {
        prefs.edit().putString(KEY_LAST_POLLING_STATUS, status).apply()
        prefs.edit().putString(KEY_LAST_POLLING_TIME, SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())).apply()
    }
    fun getLastPolling(): String {
        val status = prefs.getString(KEY_LAST_POLLING_STATUS, "Sin estado") ?: "Sin estado"
        val time = prefs.getString(KEY_LAST_POLLING_TIME, "") ?: ""
        return if (time.isNotEmpty()) "$status ($time)" else status
    }

    fun saveLastSmsReceived(from: String, text: String) {
        val truncatedText = if (text.length > 25) text.take(25) + "..." else text
        prefs.edit().putString(KEY_LAST_SMS_RECEIVED, "$from: $truncatedText").apply()
    }
    fun getLastSmsReceived(): String = prefs.getString(KEY_LAST_SMS_RECEIVED, "Ninguno") ?: "Ninguno"

    fun saveLastSmsSent(to: String, text: String, success: Boolean) {
        val truncatedText = if (text.length > 25) text.take(25) + "..." else text
        val suffix = if (success) "Éxito" else "Fallo"
        prefs.edit().putString(KEY_LAST_SMS_SENT, "$to: $truncatedText ($suffix)").apply()
    }
    fun getLastSmsSent(): String = prefs.getString(KEY_LAST_SMS_SENT, "Ninguno") ?: "Ninguno"

    fun clearDiagnostics() {
        prefs.edit()
            .remove(KEY_LAST_HEARTBEAT_STATUS)
            .remove(KEY_LAST_HEARTBEAT_TIME)
            .remove(KEY_LAST_POLLING_STATUS)
            .remove(KEY_LAST_POLLING_TIME)
            .remove(KEY_LAST_SMS_RECEIVED)
            .remove(KEY_LAST_SMS_SENT)
            .remove(KEY_LAST_API_ERROR)
            .remove(KEY_LAST_ANDROID_ERROR)
            .remove(KEY_SERVER_EVENTS)
            .apply()
    }

    // Backwards compatibility legacy properties mapping
    fun isForwardInbound(): Boolean = isReceiveEnabled()
    fun setForwardInbound(enabled: Boolean) = setReceiveEnabled(enabled)

    // DB edits
    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        smsLogDao.deleteAllSMSLogs()
    }

    suspend fun deleteLogById(id: Long) = withContext(Dispatchers.IO) {
        smsLogDao.deleteSMSLogById(id)
    }

    // SIM Cards detection models & structures
    data class SimCardInfo(
        val slot: Int,
        val subscriptionId: Int,
        val carrierName: String,
        val countryIso: String,
        val phoneNumber: String,
        val active: Boolean
    )

    fun getActiveSimCards(context: Context): List<SimCardInfo> {
        val simList = mutableListOf<SimCardInfo>()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return simList
        }

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subscriptionManager != null) {
                val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                if (activeSubscriptionInfoList != null) {
                    for (info in activeSubscriptionInfoList) {
                        val slot = info.simSlotIndex + 1
                        val subId = info.subscriptionId
                        val carrier = info.carrierName?.toString() ?: "Desconocido"
                        val country = info.countryIso ?: ""
                        
                        var phone = ""
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                phone = subscriptionManager.getPhoneNumber(subId) ?: ""
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        if (phone.isEmpty()) {
                            phone = info.number ?: ""
                        }

                        simList.add(
                            SimCardInfo(
                                slot = slot,
                                subscriptionId = subId,
                                carrierName = carrier,
                                countryIso = country,
                                phoneNumber = phone,
                                active = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SimHelper", "Error getting subscription list", e)
        }

        return simList
    }

    fun getSubscriptionIdForSlot(context: Context, slot: Int): Int {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return -1

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subscriptionManager != null) {
                val activeList = subscriptionManager.activeSubscriptionInfoList
                if (activeList != null) {
                    for (info in activeList) {
                        if (info.simSlotIndex == slot - 1) {
                            return info.subscriptionId
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SimHelper", "Error getting subscription ID for slot $slot", e)
        }
        return -1
    }

    fun getCarrierNameForSlot(context: Context, slot: Int): String {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return "Desconocido"

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subscriptionManager != null) {
                val activeList = subscriptionManager.activeSubscriptionInfoList
                if (activeList != null) {
                    for (info in activeList) {
                        if (info.simSlotIndex == slot - 1) {
                            return info.carrierName?.toString() ?: "Desconocido"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SimHelper", "Error getting carrier name for slot $slot", e)
        }
        return "Desconocido"
    }

    fun getSlotForSubscriptionId(context: Context, subscriptionId: Int): Int {
        if (subscriptionId == -1) return 1
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return 1

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subscriptionManager != null) {
                val activeList = subscriptionManager.activeSubscriptionInfoList
                if (activeList != null) {
                    for (info in activeList) {
                        if (info.subscriptionId == subscriptionId) {
                            return info.simSlotIndex + 1
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SimHelper", "Error getting slot for subscriptionId $subscriptionId", e)
        }
        return 1
    }

    // Monitoring helpers
    fun getBatteryLevel(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    }

    fun isBatteryCharging(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        }
        return false
    }

    // Send SMS logic supporting Dual SIM and Status Callback
    suspend fun sendSMS(
        to: String,
        message: String,
        gatewayMode: String,
        simSlot: Int = 1,
        remoteId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val modeStr = if (remoteId != null) "${gatewayMode}_$remoteId" else gatewayMode
        val logEntity = SMSLog(
            phoneNumber = to,
            message = message,
            type = "OUTBOUND",
            status = "PENDING",
            gatewayMode = modeStr
        )
        val logId = smsLogDao.insertSMSLog(logEntity)

        if (isSimulationMode()) {
            delaySimulation()
            smsLogDao.updateStatus(logId, "SENT", "Enviado con éxito (Simulado)")
            saveLastSmsSent(to, message, true)
            appendServerEvent("SIMULADO: SMS a $to")
            if (remoteId != null) {
                reportMessageStatusToServer(remoteId, "sent", simSlot, null)
            }
            return@withContext true
        }

        try {
            val hasPermission = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                val errMsg = "Error: Permiso de SEND_SMS no concedido."
                smsLogDao.updateStatus(logId, "FAILED", errMsg)
                saveAndroidError("Falta permiso SEND_SMS")
                saveLastSmsSent(to, message, false)
                appendServerEvent("FALLÓ: permiso SEND_SMS ausente")
                if (remoteId != null) {
                    reportMessageStatusToServer(remoteId, "failed", simSlot, errMsg)
                }
                return@withContext false
            }

            val subId = getSubscriptionIdForSlot(context, simSlot)
            val sentIntent = PendingIntent.getBroadcast(
                context,
                logId.toInt(),
                Intent("com.example.SMS_SENT_ACTION").apply {
                    putExtra("log_id", logId)
                    putExtra("remote_id", remoteId)
                    putExtra("sim_slot", simSlot)
                    putExtra("phoneNumber", to)
                    setPackage(context.packageName)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0
            )

            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && subId != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>()
                for (part in parts) {
                    sentIntents.add(sentIntent)
                }
                smsManager.sendMultipartTextMessage(to, null, parts, sentIntents, null)
            } else {
                smsManager.sendTextMessage(to, null, message, sentIntent, null)
            }

            Log.d("GatewayRepository", "SmsManager initiated send for to=$to, remoteId=$remoteId")
            appendServerEvent("ENVIANDO: SMS a $to por SIM $simSlot")
            return@withContext true
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "Error desconocido al enviar"
            smsLogDao.updateStatus(logId, "FAILED", errMsg)
            saveAndroidError("Envío fallido: $errMsg")
            saveLastSmsSent(to, message, false)
            if (remoteId != null) {
                reportMessageStatusToServer(remoteId, "failed", simSlot, errMsg)
            }
            return@withContext false
        }
    }

    // Inbound handling
    suspend fun logInboundSMS(from: String, text: String, subscriptionId: Int) = withContext(Dispatchers.IO) {
        val slot = getSlotForSubscriptionId(context, subscriptionId)
        val carrier = getActiveProvider(context, slot)

        val logEntity = SMSLog(
            phoneNumber = from,
            message = text,
            type = "INBOUND",
            status = "SENT",
            gatewayMode = "LOCAL_RECEIVER_SIM_$slot"
        )
        smsLogDao.insertSMSLog(logEntity)
        saveLastSmsReceived(from, text)

        if (isReceiveEnabled()) {
            forwardInboundToAllsender(from, text, slot, carrier)
        }
    }

    private fun forwardInboundToAllsender(from: String, text: String, slot: Int, carrier: String) {
        val baseUrl = getBaseUrl()
        val token = getDeviceToken()
        if (token.isBlank() || baseUrl.isBlank()) return

        val url = getForwardUrl()

        val json = JSONObject().apply {
            put("token", token)
            put("from", from)
            put("message", text)
            put("sim_slot", slot)
            put("provider", carrier)
            put("timestamp", System.currentTimeMillis())
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("GatewayRepository", "Error forwarding inbound message to Allsender: ${e.message}")
                saveApiError("Fallo reenvío entrante: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    Log.d("GatewayRepository", "Inbound forward responded code: ${it.code}")
                    if (!it.isSuccessful) {
                        saveApiError("Error de reenvío entrante: Código ${it.code}")
                    }
                }
            }
        })
    }

    // Polling cycles
    suspend fun pollPendingSMS(): Int = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        val token = getDeviceToken()
        if (token.isBlank() || baseUrl.isBlank()) {
            return@withContext 0
        }

        val basePolling = getPollingUrl()
        val url = if (basePolling.contains("?")) "$basePolling&token=$token" else "$basePolling?token=$token"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        var messagesProcessed = 0
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return@withContext 0
                    Log.d("GatewayRepository", "Polling response: $bodyString")
                    val jsonResponse = JSONObject(bodyString)
                    
                    if (jsonResponse.optBoolean("ok", false)) {
                        val messagesArray = jsonResponse.optJSONArray("messages")
                        val count = messagesArray?.length() ?: 0
                        saveLastPollingStatus("Éxito ($count msgs)")
                        if (count > 0) {
                            appendServerEvent("SINCRONIZACIÓN: $count SMS tomados del panel")
                        } else {
                            appendServerEvent("SINCRONIZACIÓN: conectado, sin mensajes nuevos")
                        }
                        if (messagesArray != null && count > 0) {
                            for (i in 0 until count) {
                                val msgObj = messagesArray.getJSONObject(i)
                                val id = msgObj.optString("id", "")
                                val to = msgObj.optString("to", msgObj.optString("phone_number", ""))
                                val text = msgObj.optString("message", msgObj.optString("body", ""))
                                val slot = msgObj.optInt("sim_slot", getDefaultSimSlot())

                                if (to.isNotBlank() && text.isNotBlank() && id.isNotBlank()) {
                                    if (!isSendEnabled()) {
                                        reportMessageStatusToServer(id, "failed", slot, "Envío deshabilitado en ajustes de la app")
                                        continue
                                    }

                                    val isMarketing = msgObj.optString("type", "").contains("marketing", ignoreCase = true)
                                    if (isMarketing && !isMarketingEnabled()) {
                                        reportMessageStatusToServer(id, "failed", slot, "SMS de marketing deshabilitado en ajustes de la app")
                                        continue
                                    }

                                    val success = sendSMS(to, text, "POLLING_CLIENT", slot, id)
                                    if (success) {
                                        messagesProcessed++
                                    }
                                }
                            }
                        }
                    } else {
                        saveLastPollingStatus("Fallo (Respuesta falsa de API)")
                        saveApiError("Polling falló: API devolvió ok=false")
                    }
                } else {
                    Log.e("GatewayRepository", "Polling failed with code: ${response.code}")
                    saveLastPollingStatus("Error ${response.code}")
                    saveApiError("Polling falló: Código ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("GatewayRepository", "Polling error: ${e.message}")
            saveLastPollingStatus("Fallo de red")
            saveApiError("Error de Red de Polling: ${e.message}")
        }
        return@withContext messagesProcessed
    }

    // Heartbeats
    suspend fun sendHeartbeat(): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl()
        val token = getDeviceToken()
        if (token.isBlank() || baseUrl.isBlank()) {
            return@withContext false
        }

        val url = getHeartbeatUrl()
        
        val simsArray = JSONArray()
        val simsList = getActiveSimCards(context)
        for (sim in simsList) {
            val sObj = JSONObject().apply {
                put("slot", sim.slot)
                put("subscription_id", sim.subscriptionId)
                put("carrier_name", getActiveProvider(context, sim.slot))
                put("country_iso", sim.countryIso)
                put("phone_number", sim.phoneNumber)
                put("active", sim.active)
            }
            simsArray.put(sObj)
        }

        val batteryLevel = getBatteryLevel(context)
        val isCharging = isBatteryCharging(context)
        val online = isNetworkAvailable(context)

        val json = JSONObject().apply {
            put("token", token)
            put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("device_model", Build.MODEL)
            put("android_version", Build.VERSION.RELEASE)
            put("app_version", "1.0.0")
            put("battery_level", batteryLevel)
            put("charging", isCharging)
            put("simulation_mode", isSimulationMode())
            put("polling_enabled", isPollingEnabled())
            put("send_enabled", isSendEnabled())
            put("receive_enabled", isReceiveEnabled())
            put("marketing_enabled", isMarketingEnabled())
            put("network_online", online)
            put("provider", getActiveProvider(context, getDefaultSimSlot()))
            put("sims", simsArray)
            put("default_sim_slot", getDefaultSimSlot())
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    Log.d("GatewayRepository", "Heartbeat success: $bodyStr")
                    val jsonResponse = JSONObject(bodyStr)
                    val okStatus = jsonResponse.optBoolean("ok", false)
                    if (okStatus) {
                        saveLastHeartbeatStatus("Éxito")
                        appendServerEvent("CONECTADO: heartbeat OK")

                        // FAILSAFE CRITICAL:
                        // If the phone is successfully sending heartbeat, it must also poll.
                        // This guarantees web queued SMS are collected even if a separate polling loop
                        // was not started by Android/background restrictions.
                        try {
                            setPollingEnabled(true)
                            val heartbeatPollCount = pollPendingSMS()
                            Log.d("GatewayRepository", "Heartbeat-triggered polling executed. Processed: $heartbeatPollCount")
                        } catch (pollError: Exception) {
                            Log.e("GatewayRepository", "Heartbeat-triggered polling error: ${pollError.message}", pollError)
                            saveApiError("Heartbeat polling error: ${pollError.message}")
                        }
                    } else {
                        saveLastHeartbeatStatus("Fallo API")
                        appendServerEvent("DESCONECTADO: heartbeat falló")
                        saveApiError("Heartbeat falló: API devolvió ok=false")
                    }
                    return@withContext okStatus
                } else {
                    Log.e("GatewayRepository", "Heartbeat failed with code: ${response.code}")
                    saveLastHeartbeatStatus("Error ${response.code}")
                    saveApiError("Heartbeat falló: Código ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("GatewayRepository", "Error sending heartbeat: ${e.message}")
            saveLastHeartbeatStatus("Fallo de red")
            appendServerEvent("DESCONECTADO: fallo de red heartbeat")
            saveApiError("Error de Red de Heartbeat: ${e.message}")
        }
        return@withContext false
    }

    // Reporting sending outcome to main panel
    fun reportMessageStatusToServer(msgId: String, status: String, simSlot: Int, errorMessage: String?) {
        val baseUrl = getBaseUrl()
        val token = getDeviceToken()
        if (token.isBlank() || baseUrl.isBlank()) return

        val url = getStatusUrl()
        val carrier = getActiveProvider(context, simSlot)

        val json = JSONObject().apply {
            put("token", token)
            put("id", msgId)
            put("status", status.lowercase())
            put("sim_slot", simSlot)
            put("provider", carrier)
            if (status.lowercase() == "sent") {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                put("sent_at", dateFormat.format(Date()))
            } else {
                put("error", errorMessage ?: "Unknown error")
            }
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("GatewayRepository", "Failed to report status to Allsender for $msgId: ${e.message}")
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
            }
        })
    }

    private suspend fun delaySimulation() {
        withContext(Dispatchers.IO) {
            val randomDelay = (800..2000).random().toLong()
            kotlinx.coroutines.delay(randomDelay)
        }
    }
}
