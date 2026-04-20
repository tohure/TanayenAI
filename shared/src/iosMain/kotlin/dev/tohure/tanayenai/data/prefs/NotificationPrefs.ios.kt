package dev.tohure.tanayenai.data.prefs

import dev.tohure.tanayenai.domain.model.NotificationSettings
import platform.Foundation.NSUserDefaults

actual class NotificationPrefs {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun save(settings: NotificationSettings) {
        defaults.setBool(settings.morningEnabled, "morning_enabled")
        defaults.setInteger(settings.morningHour.toLong(), "morning_hour")
        defaults.setInteger(settings.morningMinute.toLong(), "morning_minute")
    }

    actual fun load(userId: String) =
        NotificationSettings(
            userId = userId,
            morningEnabled = defaults.boolForKey("morning_enabled"),
            morningHour =
                if (defaults.objectForKey("morning_hour") != null) {
                    defaults.integerForKey("morning_hour").toInt().coerceIn(0, 23)
                } else {
                    7
                },
            morningMinute =
                if (defaults.objectForKey("morning_minute") != null) {
                    defaults.integerForKey("morning_minute").toInt().coerceIn(0, 59)
                } else {
                    0
                },
        )
}
