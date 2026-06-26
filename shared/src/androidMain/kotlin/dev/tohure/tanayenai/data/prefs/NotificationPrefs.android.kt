package dev.tohure.tanayenai.data.prefs

import android.content.Context
import androidx.core.content.edit
import dev.tohure.tanayenai.domain.model.NotificationSettings

actual class NotificationPrefs(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences("tanayen_prefs", Context.MODE_PRIVATE)

    actual fun save(settings: NotificationSettings) {
        prefs.edit {
            putBoolean("morning_enabled", settings.morningEnabled)
            putInt("morning_hour", settings.morningHour)
            putInt("morning_minute", settings.morningMinute)
        }
    }

    actual fun load(userId: String) =
        NotificationSettings(
            userId = userId,
            morningEnabled = prefs.getBoolean("morning_enabled", true),
            morningHour = prefs.getInt("morning_hour", 7),
            morningMinute = prefs.getInt("morning_minute", 0),
        )

    actual fun saveDisplayName(name: String) {
        prefs.edit { putString("display_name", name) }
    }

    actual fun loadDisplayName(): String? = prefs.getString("display_name", null)
}
