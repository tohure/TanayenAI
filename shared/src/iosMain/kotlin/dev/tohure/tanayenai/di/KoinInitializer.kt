package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.data.health.HealthDataReader
import dev.tohure.tanayenai.data.local.DatabaseDriverFactory
import dev.tohure.tanayenai.data.pdf.PdfPicker
import dev.tohure.tanayenai.data.prefs.NotificationPrefs
import dev.tohure.tanayenai.domain.model.GeminiConfig
import dev.tohure.tanayenai.domain.usecase.GenerateMorningAdviceUseCase
import dev.tohure.tanayenai.domain.usecase.SyncHealthMetricsUseCase
import dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel
import dev.tohure.tanayenai.presentation.viewmodel.ClinicalProfileViewModel
import dev.tohure.tanayenai.presentation.viewmodel.DashboardViewModel
import dev.tohure.tanayenai.presentation.viewmodel.FoodDiaryViewModel
import dev.tohure.tanayenai.presentation.viewmodel.NotificationSettingsViewModel
import dev.tohure.tanayenai.presentation.viewmodel.PantryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.experimental.ExperimentalObjCName

// Scope compartido para operaciones fire-and-forget en iOS.
// SupervisorJob garantiza que un fallo en una coroutine no cancela las demás.
private val iosSupportScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun initKoin(geminiApiKey: String) {
    startKoin {
        modules(
            sharedModules() +
                module {
                    single { DatabaseDriverFactory() }
                    single { GeminiConfig(geminiApiKey) }
                    single { HealthDataReader() }
                    single { PdfPicker() }
                    single { NotificationPrefs() }
                },
        )
    }
}

@Suppress("unused") // Called from Swift
fun getDashboardViewModel(userId: String): DashboardViewModel =
    KoinPlatform.getKoin().get<DashboardViewModel> { parametersOf(userId) }

@Suppress("unused") // Called from Swift
fun getChatViewModel(userId: String): ChatViewModel = KoinPlatform.getKoin().get<ChatViewModel> { parametersOf(userId) }

@Suppress("unused") // Called from Swift
fun getClinicalProfileViewModel(userId: String): ClinicalProfileViewModel =
    KoinPlatform.getKoin().get<ClinicalProfileViewModel> { parametersOf(userId) }

@Suppress("unused") // Called from Swift
fun getPantryViewModel(userId: String): PantryViewModel =
    KoinPlatform.getKoin().get<PantryViewModel> { parametersOf(userId) }

@Suppress("unused") // Called from Swift
fun getFoodDiaryViewModel(userId: String): FoodDiaryViewModel =
    KoinPlatform.getKoin().get<FoodDiaryViewModel> { parametersOf(userId) }

@Suppress("unused") // Called from Swift
fun getPdfPicker(): PdfPicker = KoinPlatform.getKoin().get()

@Suppress("unused") // Called from Swift
fun getNotificationSettingsViewModel(
    userId: String,
    onScheduleChanged: (Int, Int, Boolean) -> Unit,
): NotificationSettingsViewModel = KoinPlatform.getKoin().get { parametersOf(userId, onScheduleChanged) }

@Suppress("unused") // Called from Swift
fun getMorningAdviceUseCase(userId: String): GenerateMorningAdviceUseCase =
    KoinPlatform.getKoin().get { parametersOf(userId) }

@Suppress("unused") // Called from Swift
fun getNotificationPrefs(): NotificationPrefs = KoinPlatform.getKoin().get()

@OptIn(ExperimentalObjCName::class)
@ObjCName(name = "requestHealthPermissionsFromIos")
@Suppress("unused") // Called from Swift
fun requestHealthPermissionsFromIos(onResult: (Boolean) -> Unit) {
    iosSupportScope.launch(Dispatchers.Main) {
        try {
            val dataReader = KoinPlatform.getKoin().get<HealthDataReader>()
            val result = dataReader.requestPermissionsIos(SyncHealthMetricsUseCase.requiredPermissions)
            onResult(result)
        } catch (e: Exception) {
            println("=== IOS PERMISSIONS ERROR: ${e.message}")
            onResult(false)
        }
    }
}
