package dev.tohure.tanayenai.data.prefs

import dev.tohure.tanayenai.domain.model.NotificationSettings

expect class NotificationPrefs {
    fun save(settings: NotificationSettings)

    fun load(userId: String): NotificationSettings
}
