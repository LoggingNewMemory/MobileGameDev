package amba.dojo.pulse

import android.content.Context
import android.content.SharedPreferences
import amba.dojo.pulse.vision.PunchType
import amba.dojo.pulse.vision.TrainingTelemetry
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data satu sesi latihan yang sudah selesai.
 */
data class TrainingSession(
    val timestampMs: Long,          // kapan sesi selesai
    val totalStrikes: Int,          // total pukulan
    val formScore: Int,             // 0-100
    val durationSeconds: Int,       // durasi latihan (detik)
    val peakVelocityMs: Float,      // kecepatan puncak m/s (dari telemetry)
    val strikeCounts: Map<String, Int> // per-tipe pukulan
) {
    val dateLabel: String get() = SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(timestampMs))
    val dayLabel: String  get() = SimpleDateFormat("EEE", Locale("id")).format(Date(timestampMs))
}

/**
 * Statistik agregat mingguan dihitung dari daftar sesi.
 */
data class WeeklyStats(
    val sessions: List<TrainingSession>,
    val totalStrikes: Int,
    val avgFormScore: Int,
    val totalDurationSeconds: Int,
    val avgVelocityMs: Float,
    val peakVelocityMs: Float,
    // 7 bar chart values (0f–1f), Mon=index 0 … Sun=index 6
    val weeklyBarValues: List<Float>,
    val weeklyBarHighlights: Set<Int>  // indeks bar dengan nilai tertinggi
) {
    val matTimeLabel: String get() {
        val h = totalDurationSeconds / 3600
        val m = (totalDurationSeconds % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
    val workoutsThisWeek: Int get() = sessions.size
}

/**
 * Singleton repository. Gunakan [init] sekali di Application/Activity onCreate.
 */
object TrainingRepository {

    private const val PREFS_NAME   = "dojo_training_prefs"
    private const val KEY_SESSIONS = "sessions_json"
    private const val MAX_SESSIONS = 60      // simpan 60 sesi terakhir

    private lateinit var prefs: SharedPreferences

    // ── Reactive state ───────────────────────────────────────────────────────
    private val _sessions = MutableStateFlow<List<TrainingSession>>(emptyList())
    val sessions: StateFlow<List<TrainingSession>> = _sessions.asStateFlow()

    // ── Init ─────────────────────────────────────────────────────────────────
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _sessions.value = loadAll()
    }

    // ── Save a completed session ─────────────────────────────────────────────
    fun save(telemetry: TrainingTelemetry, durationSeconds: Int) {
        // Estimate peak velocity from punch counts (approximation: use formScore proxy)
        val peakVelocity = (telemetry.formScore * 0.3f).coerceIn(5f, 35f)

        val session = TrainingSession(
            timestampMs     = System.currentTimeMillis(),
            totalStrikes    = telemetry.totalStrikes,
            formScore       = telemetry.formScore,
            durationSeconds = durationSeconds.coerceAtLeast(1),
            peakVelocityMs  = peakVelocity,
            strikeCounts    = telemetry.strikeCounts.mapKeys { it.key.name }
        )
        val updated = (_sessions.value + session).takeLast(MAX_SESSIONS)
        _sessions.value = updated
        persist(updated)
    }

    // ── Computed weekly stats ────────────────────────────────────────────────
    fun weeklyStats(): WeeklyStats {
        val all = _sessions.value
        // Last 7 days
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val week   = all.filter { it.timestampMs >= cutoff }

        if (week.isEmpty()) return WeeklyStats(
            sessions = emptyList(),
            totalStrikes = 0, avgFormScore = 0,
            totalDurationSeconds = 0, avgVelocityMs = 0f, peakVelocityMs = 0f,
            weeklyBarValues = List(7) { 0f }, weeklyBarHighlights = emptySet()
        )

        // Per-day aggregation (Mon=0 … Sun=6)
        val strikesByDay = IntArray(7)
        val cal = java.util.Calendar.getInstance()
        week.forEach { s ->
            cal.timeInMillis = s.timestampMs
            val dow = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0
            strikesByDay[dow] += s.totalStrikes
        }
        val maxStrikes = strikesByDay.max().coerceAtLeast(1)
        val barValues  = strikesByDay.map { it.toFloat() / maxStrikes }
        val highlights = strikesByDay.indices.filter { strikesByDay[it] == strikesByDay.max() }.toSet()

        return WeeklyStats(
            sessions             = week,
            totalStrikes         = week.sumOf { it.totalStrikes },
            avgFormScore         = week.map { it.formScore }.average().toInt(),
            totalDurationSeconds = week.sumOf { it.durationSeconds },
            avgVelocityMs        = week.map { it.peakVelocityMs }.average().toFloat(),
            peakVelocityMs       = week.maxOf { it.peakVelocityMs },
            weeklyBarValues      = barValues,
            weeklyBarHighlights  = highlights
        )
    }

    // ── Persist / load ───────────────────────────────────────────────────────
    private fun persist(sessions: List<TrainingSession>) {
        val arr = JSONArray()
        sessions.forEach { s ->
            val obj = JSONObject().apply {
                put("ts",  s.timestampMs)
                put("str", s.totalStrikes)
                put("fs",  s.formScore)
                put("dur", s.durationSeconds)
                put("vel", s.peakVelocityMs.toDouble())
                val counts = JSONObject()
                s.strikeCounts.forEach { (k, v) -> counts.put(k, v) }
                put("counts", counts)
            }
            arr.put(obj)
        }
        prefs.edit { putString(KEY_SESSIONS, arr.toString()) }
    }

    private fun loadAll(): List<TrainingSession> {
        val json = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val counts = mutableMapOf<String, Int>()
                val countsObj = o.optJSONObject("counts")
                countsObj?.keys()?.forEach { k -> counts[k] = countsObj.getInt(k) }
                TrainingSession(
                    timestampMs     = o.getLong("ts"),
                    totalStrikes    = o.getInt("str"),
                    formScore       = o.getInt("fs"),
                    durationSeconds = o.getInt("dur"),
                    peakVelocityMs  = o.getDouble("vel").toFloat(),
                    strikeCounts    = counts
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}
