package com.carlos.finanzas.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Un SMS largo llega en partes del mismo remitente: se unen.
        messages.groupBy { it.originatingAddress.orEmpty() }.forEach { (sender, parts) ->
            val body = parts.joinToString("") { it.messageBody.orEmpty() }
            val ts = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
            CaptureProcessor.process(context, "SMS", sender, body, ts)
        }
    }
}
