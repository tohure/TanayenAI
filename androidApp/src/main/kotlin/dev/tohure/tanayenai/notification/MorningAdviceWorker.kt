package dev.tohure.tanayenai.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.tohure.tanayenai.R
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.domain.usecase.GenerateMorningAdviceUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class MorningAdviceWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params),
    KoinComponent {
    private val generateAdvice: GenerateMorningAdviceUseCase by inject { parametersOf(PROTOTYPE_USER_ID) }

    override suspend fun doWork(): Result {
        return try {
            val advice = generateAdvice.generate() ?: return Result.success()
            showNotification(advice.title, advice.body)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    private fun showNotification(
        title: String,
        body: String,
    ) {
        val channelId = "morning_advice"
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel =
            NotificationChannel(
                channelId,
                "Consejo matutino",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Consejo nutricional diario personalizado" }
        manager.createNotificationChannel(channel)

        val notification =
            NotificationCompat
                .Builder(appContext, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .build()

        manager.notify(1001, notification)
    }
}
