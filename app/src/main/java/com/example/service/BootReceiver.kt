package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.GatewayRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        try {
            val repository = GatewayRepository(context)
            if (!repository.isServerModeEnabled()) {
                Log.d("BootReceiver", "Server mode disabled. Not restarting gateway.")
                return
            }

            repository.appendServerEvent("AUTO-INICIO: reiniciando servidor SMS después de arranque/actualización")

            val serviceIntent = Intent(context, GatewayService::class.java).apply {
                this.action = GatewayService.ACTION_START
            }

            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e("BootReceiver", "Could not restart service: ${e.message}", e)
        }
    }
}
