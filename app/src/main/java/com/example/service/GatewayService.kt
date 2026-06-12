package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.os.PowerManager
import android.app.AlarmManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.GatewayRepository
import com.example.data.SMSLog
import com.example.data.AppDatabase
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class GatewayService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private lateinit var repository: GatewayRepository
    private var httpServer: HttpServer? = null
    private var pollingJob: Job? = null
    private var heartbeatJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val smsSentResultReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.SMS_SENT_ACTION") {
                val logId = intent.getLongExtra("log_id", -1)
                val remoteId = intent.getStringExtra("remote_id")
                val simSlot = intent.getIntExtra("sim_slot", 1)
                val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""

                val resultCode = resultCode
                val status = if (resultCode == Activity.RESULT_OK) "SENT" else "FAILED"
                val errorDescription = when (resultCode) {
                    Activity.RESULT_OK -> "Enviado con éxito"
                    android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Fallo genérico"
                    android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> "Sin servicio de red"
                    android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
                    android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> "Modem/Radio apagado"
                    else -> "Error de red móvil: $resultCode"
                }

                Log.d(TAG, "Sent SMS broadcast callback: logId=$logId, status=$status, error=$errorDescription")

                serviceScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    db.smsLogDao().updateStatus(logId, status, errorDescription)
                    
                    repository.saveLastSmsSent(phoneNumber, errorDescription, status == "SENT")
                    repository.appendServerEvent(if (status == "SENT") "ENVIADO: $phoneNumber" else "FALLÓ: $phoneNumber - $errorDescription")
                    if (status != "SENT") {
                        repository.saveAndroidError("Fallo SMS a $phoneNumber: $errorDescription")
                    }
                    
                    if (remoteId != null) {
                        repository.reportMessageStatusToServer(remoteId, status.lowercase(), simSlot, if (status == "FAILED") errorDescription else null)
                    }
                }
            }
        }
    }
    
    companion object {
        private const val CHANNEL_ID = "allsender_gateway_id"
        private const val NOTIFICATION_ID = 2503
        private const val TAG = "GatewayService"
        
        // Actions to control service state
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        repository = GatewayRepository(this)
        createNotificationChannel()

        // Dynamic registration of SMS Sent receiver
        val filter = android.content.IntentFilter("com.example.SMS_SENT_ACTION")
        ContextCompat.registerReceiver(this, smsSentResultReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand with action: $action")
        
        if (action == ACTION_STOP) {
            stopGatewayService()
        } else {
            startGatewayService()
        }
        
        return START_STICKY
    }

    private fun startGatewayService() {
        // Build Foreground Notification
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Allsender SMS Gateway")
            .setContentText("Servidor activo: sincronizando SMS en segundo plano.")
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // CRITICAL: If foreground start fails, do NOT continue as active.
        if (!startForegroundSafely(notification)) {
            repository.setServerActive(false)
            repository.saveAndroidError("No se pudo activar el servicio foreground. Revisa permisos de notificación/batería.")
            stopSelf()
            return
        }

        // Force polling ON for active gateway. Some older installs saved it as false.
        repository.setPollingEnabled(true)
        repository.setServerActive(true)
        repository.appendServerEvent("SERVIDOR ACTIVADO: teléfono encendido y sincronizando")
        repository.appendServerEvent("SEGUNDO PLANO: servicio visible con notificación permanente")
        acquireWakeLockSafely()

        // Start Local HTTP Server
        startHttpServer()

        // Start Polling loop always when gateway is active
        startPollingLoop()

        // Poll immediately so queued SMS from web are taken without waiting.
        serviceScope.launch {
            try {
                val processedNow = repository.pollPendingSMS()
                Log.d(TAG, "Immediate polling executed. Processed: $processedNow")
            } catch (e: Exception) {
                Log.e(TAG, "Immediate polling error: ${e.message}", e)
                repository.saveAndroidError("Error polling inmediato: ${e.message}")
            }
        }

        // Start periodic Heartbeat
        startHeartbeatLoop()
    }

    private fun startForegroundSafely(notification: Notification): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                )
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground service as remoteMessaging: ${e.message}", e)
                repository.saveAndroidError("Fallo startForeground remoteMessaging: ${e.message}")
                try {
                    startForeground(NOTIFICATION_ID, notification)
                    true
                } catch (ex: Exception) {
                    Log.e(TAG, "Error fallback startForeground: ${ex.message}", ex)
                    repository.saveAndroidError("Fallo startForeground fallback: ${ex.message}")
                    false
                }
            }
        } else {
            try {
                startForeground(NOTIFICATION_ID, notification)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground service: ${e.message}", e)
                repository.saveAndroidError("Fallo startForeground: ${e.message}")
                false
            }
        }
    }


    private fun acquireWakeLockSafely() {
        try {
            if (wakeLock?.isHeld == true) return

            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AllsenderSmsGateway:ServerWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 horas; se renueva al reiniciar el servicio.
            }

            repository.appendServerEvent("ENERGÍA: wakelock parcial activo para mantener sincronización")
            Log.d(TAG, "Partial WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Could not acquire wakelock: ${e.message}", e)
            repository.saveAndroidError("No se pudo activar wakelock: ${e.message}")
        }
    }

    private fun releaseWakeLockSafely() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Partial WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not release wakelock: ${e.message}", e)
        } finally {
            wakeLock = null
        }
    }

    private fun scheduleRestartIfServerModeEnabled(reason: String) {
        try {
            if (!repository.isServerModeEnabled()) return

            repository.appendServerEvent("AUTO-RECUPERACIÓN: intentando reiniciar servicio ($reason)")

            val restartIntent = Intent(applicationContext, GatewayService::class.java).apply {
                action = ACTION_START
            }

            val pendingIntent = PendingIntent.getService(
                applicationContext,
                9901,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 3000L,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not schedule restart: ${e.message}", e)
            repository.saveAndroidError("No se pudo programar reinicio: ${e.message}")
        }
    }

    private fun stopGatewayService() {
        Log.d(TAG, "Stopping service and shutting down assets...")
        repository.setServerActive(false)
        stopHttpServer()
        stopPollingLoop()
        stopHeartbeatLoop()
        releaseWakeLockSafely()
        serviceJob.cancel()
        stopForeground(true)
        stopSelf()
    }

    private fun startHttpServer() {
        stopHttpServer() // Make sure it is clean
        
        val port = repository.getPort()
        val apiKey = repository.getApiKey()
        
        try {
            httpServer = HttpServer.create(InetSocketAddress(port), 0).apply {
                // Main route - Info / Test Panel
                createContext("/", HomeHandler())
                // Sending route
                createContext("/send", SendHandler(repository, apiKey, serviceScope))
                // Status route
                createContext("/status", StatusHandler(repository))
                
                // Executor for multithreading requests
                executor = java.util.concurrent.Executors.newCachedThreadPool()
                start()
            }
            Log.d(TAG, "HTTP Server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTP server: ${e.message}")
        }
    }

    private fun stopHttpServer() {
        httpServer?.let {
            try {
                it.stop(1)
                Log.d(TAG, "HTTP Server stopped successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping HTTP server: ${e.message}")
            }
        }
        httpServer = null
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()

        // Force polling ON while the gateway service is active.
        if (!repository.isPollingEnabled()) {
            Log.w(TAG, "Polling was disabled. Enabling automatically for active gateway.")
            repository.setPollingEnabled(true)
        }

        pollingJob = serviceScope.launch {
            while (isActive) {
                Log.d(TAG, "Polling loop cycle. Checking for pending messages on Allsender...")
                try {
                    val processed = repository.pollPendingSMS()
                    if (processed > 0) {
                        Log.d(TAG, "Successfully processed $processed outbound SMS messages through Allsender.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling cycle: ${e.message}", e)
                    repository.saveAndroidError("Error polling loop: ${e.message}")
                }

                val intervalSec = repository.getPollingInterval().coerceAtLeast(5)
                delay(intervalSec * 1000L)
            }
        }
    }

    private fun stopPollingLoop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                Log.d(TAG, "Heartbeat loop cycle. Reporting state to Allsender...")
                try {
                    val ok = repository.sendHeartbeat()
                    Log.d(TAG, "Allsender Heartbeat response OK: $ok")
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat connection error: ${e.message}")
                }
                // Modo servidor: heartbeat + polling automático cada X segundos.
                // Usa el intervalo configurado en Ajustes. Mínimo 5s, máximo 60s.
                val intervalSec = repository.getPollingInterval().coerceIn(5, 60)
                delay(intervalSec * 1000L)
            }
        }
    }

    private fun stopHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        repository.appendServerEvent("APP CERRADA: servicio queda protegido y se reintentará si Android lo corta")
        scheduleRestartIfServerModeEnabled("task removed")
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(smsSentResultReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }

        stopHttpServer()
        stopPollingLoop()
        stopHeartbeatLoop()
        releaseWakeLockSafely()

        if (repository.isServerModeEnabled()) {
            scheduleRestartIfServerModeEnabled("service destroyed")
        } else {
            repository.setServerActive(false)
        }

        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal SMS Allsender Gateway",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificación permanente requerida para mantener el servidor SMS activo"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    // HANDLERS FOR THE HTTP SERVER

    // 1. Home / Information page
    private class HomeHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>Allsender Gateway Activo</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background-color: #0f172a; color: #f8fafc; padding: 40px; }
                        .card { background-color: #1e293b; border-radius: 12px; padding: 30px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); max-width: 600px; margin: auto; border: 1px solid #334155; }
                        h1 { color: #10b981; margin-top: 0; font-size: 24px; }
                        p { line-height: 1.6; color: #94a3b8; }
                        .endpoint { background-color: #0f172a; padding: 12px; border-radius: 6px; font-family: monospace; font-size: 14px; border: 1px solid #334155; color: #38bdf8; overflow-x: auto;}
                        .footer { margin-top: 20px; font-size: 12px; text-align: center; color: #64748b; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Allsender SMS Gateway</h1>
                        <p>El servidor gateway local se encuentra activo y listo para procesar peticiones HTTP de envío de mensajería instantánea.</p>
                        <p><strong>Endpoint de envío:</strong></p>
                        <div class="endpoint">
                            GET /send?to=[telefono]&message=[texto]&key=[tu_clave]
                        </div>
                        <p>También puedes realizar un POST a este endpoint enviando un cuerpo JSON:</p>
                        <div class="endpoint">
                            POST /send<br>
                            Headers: Content-Type: application/json<br>
                            Body: { "to": "+12345678", "message": "Hola mundo", "key": "tu_clave" }
                        </div>
                    </div>
                    <div class="footer">Allsender Gateway Server &copy; 2026</div>
                </body>
                </html>
            """.trimIndent()
            
            val bytes = html.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
    }

    // 2. Status Handler
    private class StatusHandler(private val repo: GatewayRepository) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
            
            val json = JSONObject().apply {
                put("status", "running")
                put("simulation_mode", repo.isSimulationMode())
                put("polling_enabled", repo.isPollingEnabled())
                put("port", repo.getPort())
            }
            
            val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
    }

    // 3. Send SMS Handler (processes GET or POST and triggers sending)
    private class SendHandler(
        private val repo: GatewayRepository,
        private val configuredKey: String,
        private val scope: CoroutineScope
    ) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            // Enable CORS
            exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
            exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type,Authorization")
            exchange.responseHeaders.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")

            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                exchange.close()
                return
            }

            var toValue = ""
            var messageValue = ""
            var keyValue = ""

            try {
                if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                    // Parse body JSON
                    val reader = BufferedReader(InputStreamReader(exchange.requestBody, StandardCharsets.UTF_8))
                    val body = reader.readText()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        toValue = json.optString("to", json.optString("phone", json.optString("number", "")))
                        messageValue = json.optString("message", json.optString("text", json.optString("msg", "")))
                        keyValue = json.optString("key", json.optString("token", json.optString("api_key", "")))
                    }
                } else if (exchange.requestMethod.equals("GET", ignoreCase = true)) {
                    // Parse query variables
                    val query = exchange.requestURI.rawQuery
                    if (query != null) {
                        val params = query.split("&")
                        for (param in params) {
                            val pair = param.split("=")
                            if (pair.size > 1) {
                                val key = URLDecoder.decode(pair[0], "UTF-8")
                                val value = URLDecoder.decode(pair[1], "UTF-8")
                                when (key) {
                                    "to", "phone", "number" -> toValue = value
                                    "message", "text", "msg" -> messageValue = value
                                    "key", "token", "api_key" -> keyValue = value
                                }
                            }
                        }
                    }
                }

                // Security Authorization validation
                if (keyValue != configuredKey) {
                    val responseJson = JSONObject().apply {
                        put("status", "error")
                        put("code", 401)
                        put("message", "Clave API inválida o ausente en la petición.")
                    }
                    sendJsonResponse(exchange, 401, responseJson)
                    return
                }

                if (toValue.isBlank() || messageValue.isBlank()) {
                    val responseJson = JSONObject().apply {
                        put("status", "error")
                        put("code", 400)
                        put("message", "Variables 'to' y 'message' obligatorias.")
                    }
                    sendJsonResponse(exchange, 400, responseJson)
                    return
                }

                // Trigger Android SMS send non-blockingly using serviceScope
                val targetPhone = toValue
                val targetMessage = messageValue

                scope.launch {
                    repo.sendSMS(targetPhone, targetMessage, "LOCAL_SERVER")
                }

                val responseJson = JSONObject().apply {
                    put("status", "success")
                    put("message", "Mensaje recibido y encolado para su envío.")
                    put("to", targetPhone)
                    put("timestamp", System.currentTimeMillis())
                }
                sendJsonResponse(exchange, 200, responseJson)

            } catch (e: Exception) {
                val responseJson = JSONObject().apply {
                    put("status", "error")
                    put("code", 500)
                    put("message", "Error interno: ${e.message}")
                }
                sendJsonResponse(exchange, 500, responseJson)
            }
        }

        private fun sendJsonResponse(exchange: HttpExchange, code: Int, json: JSONObject) {
            val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
    }
}
