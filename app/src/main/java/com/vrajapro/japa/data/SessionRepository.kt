package com.vrajapro.japa.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class Session(
    val timestamp: Long,
    val durationSec: Int,
    val mantraCount: Int,
    val notes: String,
    /** True when the session was saved automatically after inactivity. */
    val autoSaved: Boolean = false,
) {
    val rounds: Int get() = mantraCount / MANTRAS_PER_ROUND
}

const val MANTRAS_PER_ROUND = 108 // one full mala

/**
 * Stores session history on the device only (no account, no server).
 *
 * Same rules as the web app: only today's sessions are kept, capped at the
 * last [MAX_SESSIONS]. Sessions from previous days are dropped on next load.
 */
class SessionRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("japa_sessions", Context.MODE_PRIVATE)

    fun todaySessions(now: Long = System.currentTimeMillis()): List<Session> {
        val kept = readAll().filter { isSameDay(it.timestamp, now) }
        writeAll(kept)
        return kept
    }

    fun save(session: Session): List<Session> {
        val updated = (listOf(session) + todaySessions(session.timestamp)).take(MAX_SESSIONS)
        writeAll(updated)
        return updated
    }

    private fun readAll(): List<Session> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Session(
                    timestamp = o.getLong("timestamp"),
                    durationSec = o.getInt("duration_sec"),
                    mantraCount = o.getInt("mantra_count"),
                    notes = o.optString("notes", ""),
                    autoSaved = o.optBoolean("auto_saved", false),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeAll(sessions: List<Session>) {
        val arr = JSONArray()
        sessions.forEach {
            arr.put(
                JSONObject()
                    .put("timestamp", it.timestamp)
                    .put("duration_sec", it.durationSec)
                    .put("mantra_count", it.mantraCount)
                    .put("notes", it.notes)
                    .put("auto_saved", it.autoSaved)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private companion object {
        const val KEY = "sessions"
        const val MAX_SESSIONS = 10
    }
}
