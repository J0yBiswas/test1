package com.bypassbd.otp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.bypassbd.otp.data.OtpParser
import com.bypassbd.otp.data.Prefs
import com.bypassbd.otp.net.Relay

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    val settings: StateFlow<Prefs.Settings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs.Settings("", ""))

    val log: StateFlow<List<Prefs.LogEntry>> =
        prefs.log.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Transient one-line status shown under the action buttons. */
    val toast = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)

    fun clearToast() { toast.value = null }

    fun save(phone: String, pairCode: String) {
        viewModelScope.launch {
            prefs.save(phone, pairCode)
            toast.value = "Saved"
        }
    }

    /** Push whatever the user typed/pasted (a raw SMS or a bare code). */
    fun forwardManual(text: String) {
        val otp = OtpParser.extract(text) ?: text.filter { it.isDigit() }.take(8)
        if (otp.length < 4) { toast.value = "Couldn't find a 4–8 digit code in that text"; return }
        pushNow(otp, "Manual $otp")
    }

    /** Send a fixed code so the user can confirm the server + pairing are wired up. */
    fun sendTest() = pushNow("0000", "Test push")

    private fun pushNow(otp: String, logLine: String) {
        viewModelScope.launch {
            val s = prefs.snapshot()
            if (!s.isReady) { toast.value = "Finish pairing first (phone + client key)"; return@launch }
            busy.value = true
            val r = Relay.push(Constants.DEFAULT_SERVER, s.phone, s.pairCode, otp)
            busy.value = false
            toast.value = if (r.ok) "Sent ✓ ($otp)" else "Failed: ${r.message}"
            prefs.addLog(if (r.ok) "✓ $logLine" else "✗ $logLine — ${r.message}")
        }
    }
}
