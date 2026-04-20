package dev.tohure.tanayenai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.tohure.tanayenai.data.prefs.NotificationPrefs
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val prefs: NotificationPrefs by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = prefs.load(PROTOTYPE_USER_ID)
        NotificationScheduler.schedule(context, settings.morningHour, settings.morningMinute, settings.morningEnabled)
    }
}
