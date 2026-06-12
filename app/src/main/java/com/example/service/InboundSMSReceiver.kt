package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.GatewayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InboundSMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d("InboundSMSReceiver", "SMS broadcast received!")
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isEmpty()) return

                val rep = GatewayRepository(context)
                val fullMessageBody = StringBuilder()
                val sender = messages[0].displayOriginatingAddress ?: "Unknown"

                for (msg in messages) {
                    fullMessageBody.append(msg.messageBody)
                }

                val bodyText = fullMessageBody.toString()
                val subscriptionId = intent.getIntExtra("subscription", intent.getIntExtra("subId", -1))

                // Save non-blockingly
                CoroutineScope(Dispatchers.IO).launch {
                    rep.logInboundSMS(sender, bodyText, subscriptionId)
                    Log.d("InboundSMSReceiver", "Logged inbound SMS from $sender on subId $subscriptionId: $bodyText")
                }
            } catch (e: Exception) {
                Log.e("InboundSMSReceiver", "Error processing received SMS: ${e.message}")
            }
        }
    }
}
