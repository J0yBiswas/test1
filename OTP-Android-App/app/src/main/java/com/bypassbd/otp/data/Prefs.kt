package com.bypassbd.otp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ivac_otp")

/**
 * Pairing settings + a small delivery log, persisted with Jetpack DataStore.
 * The server URL is NOT stored here — it is fixed in Constants.DEFAULT_SERVER.
 */
class Prefs(private val ctx: Context) {

    private object Keys {
        val PHONE = stringPreferencesKey("phone")
        val PAIR  = stringPreferencesKey("pairCode")
        val LOG   = stringSetPreferencesKey("log")   // "epochMs|line"
    }

    data class Settings(val phone: String, val pairCode: String) {
        val isReady: Boolean get() = phone.isNotBlank() && pairCode.isNotBlank()
    }

    val settings: Flow<Settings> = ctx.dataStore.data.map { p ->
        Settings(
            phone = p[Keys.PHONE] ?: "",
            pairCode = p[Keys.PAIR] ?: ""
        )
    }

    suspend fun snapshot(): Settings = settings.first()

    suspend fun save(phone: String, pairCode: String) {
        ctx.dataStore.edit { p ->
            p[Keys.PHONE] = phone.trim()
            p[Keys.PAIR]  = pairCode.trim().uppercase()
        }
    }

    val log: Flow<List<LogEntry>> = ctx.dataStore.data.map { p ->
        (p[Keys.LOG] ?: emptySet())
            .mapNotNull { raw ->
                val i = raw.indexOf('|')
                if (i <= 0) null else LogEntry(raw.substring(0, i).toLongOrNull() ?: 0L, raw.substring(i + 1))
            }
            .sortedByDescending { it.at }
            .take(30)
    }

    suspend fun addLog(line: String) {
        ctx.dataStore.edit { p ->
            val cur = (p[Keys.LOG] ?: emptySet()).toMutableList()
            cur.add("${System.currentTimeMillis()}|$line")
            val trimmed = cur
                .sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0L }
                .take(30)
                .toSet()
            p[Keys.LOG] = trimmed
        }
    }

    data class LogEntry(val at: Long, val text: String)
}
