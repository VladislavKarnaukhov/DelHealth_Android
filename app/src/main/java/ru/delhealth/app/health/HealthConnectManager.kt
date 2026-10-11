package ru.delhealth.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/**
 * Чтение данных из Health Connect (Google Fit, Samsung Health, Garmin, Fitbit и др. пишут туда сами).
 * Чтобы добавить новый показатель: разрешение в [PERMISSIONS] и AndroidManifest, затем строка в [readDay].
 */
class HealthConnectManager(private val context: Context) {

    enum class Availability { AVAILABLE, NEEDS_INSTALL_OR_UPDATE, UNSUPPORTED }

    val availability: Availability
        get() = when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_INSTALL_OR_UPDATE
            else -> Availability.UNSUPPORTED
        }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)

    /** Сводка по дням за последние [days] дней (включая сегодня). Нет данных → поле отсутствует. */
    suspend fun readSummary(days: Int): JSONObject {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val list = JSONArray()
        for (i in (days - 1) downTo 0) list.put(readDay(today.minusDays(i.toLong()), zone))
        return JSONObject().put("source", "health_connect").put("days", list)
    }

    private suspend fun readDay(date: LocalDate, zone: ZoneId): JSONObject {
        val range = TimeRangeFilter.between(
            date.atStartOfDay(zone).toInstant(),
            date.plusDays(1).atStartOfDay(zone).toInstant(),
        )
        val agg: AggregationResult = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    SleepSessionRecord.SLEEP_DURATION_TOTAL,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                    HeartRateRecord.BPM_AVG,
                ),
                timeRangeFilter = range,
            )
        )
        return JSONObject().apply {
            put("date", date.toString())
            agg[StepsRecord.COUNT_TOTAL]?.let { put("steps", it) }
            agg[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.let { put("sleep_minutes", it.toMinutes()) }
            agg[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.let { put("exercise_minutes", it.toMinutes()) }
            agg[HeartRateRecord.BPM_AVG]?.let { put("heart_rate_avg", it) }
        }
    }

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
        )

        fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    }
}
