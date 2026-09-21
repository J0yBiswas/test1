package com.bypassbd.otp.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.bypassbd.otp.Constants
import com.bypassbd.otp.Notifier
import com.bypassbd.otp.data.OtpParser
import com.bypassbd.otp.data.Prefs
import com.bypassbd.otp.net.Relay

/**
 * Fires on every inbound SMS (registered in the manifest, so it works even when
 * the app is closed). It reassembles multipart messages, keeps only IVAC OTP
 * texts, converts the word-form code to digits, and forwards it to the fixed relay.
 * We never abort the broadcast, so the user's normal SMS app still shows the text.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (msgs.isEmpty()) return

        val sender = msgs.first().originatingAddress
        val body = buildString { msgs.forEach { append(it.messageBody ?: "") } }

        if (!OtpParser.looksLikeIvac(sender, body)) return
        val otp = OtpParser.extract(body) ?: return

        // Network + DataStore are async — keep the receiver alive with goAsync().
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = Prefs(app)
                val s = prefs.snapshot()
                if (!s.isReady) {
                    Notifier.show(app, "OTP not forwarded",
                        "Open the app and finish pairing (phone + client key).")
                    prefs.addLog("⚠ $otp captured but app is not paired yet")
                    return@launch
                }
                val r = Relay.push(Constants.DEFAULT_SERVER, s.phone, s.pairCode, otp)
                if (r.ok) {
                    Notifier.show(app, "OTP forwarded ✓", "Code $otp sent to your extension.")
                    prefs.addLog("✓ Forwarded $otp")
                } else {
                    Notifier.show(app, "OTP send failed", r.message)
                    prefs.addLog("✗ $otp — ${r.message}")
                }
            } catch (e: Exception) {
                Notifier.show(app, "OTP error", e.message ?: "Unknown error")
            } finally {
                pending.finish()
            }
        }
    }
}
