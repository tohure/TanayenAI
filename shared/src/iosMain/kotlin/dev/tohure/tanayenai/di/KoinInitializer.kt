package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.data.health.HealthDataReader
import dev.tohure.tanayenai.data.local.DatabaseDriverFactory
import dev.tohure.tanayenai.data.remote.SyncManager
import dev.tohure.tanayenai.domain.usecase.SyncHealthMetricsUseCase
import dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel
import dev.tohure.tanayenai.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.experimental.ExperimentalObjCName

fun initKoin(
    supabaseUrl: String,
    supabaseAnonKey: String,
    geminiApiKey: String,
) {
    startKoin {
        modules(
            sharedModules() +
                module {
                    single { DatabaseDriverFactory() }
                    single(named("SUPABASE_URL")) { supabaseUrl }
                    single(named("SUPABASE_ANON_KEY")) { supabaseAnonKey }
                    single(named("GEMINI_API_KEY")) { geminiApiKey }
                    single { HealthDataReader() }
                },
        )
    }

    // Removed GlobalScope block because iOS kills it
}

@OptIn(ExperimentalObjCName::class)
@ObjCName(name = "triggerSyncFromIos")
@Suppress("unused") // Called from Swift
fun triggerSyncFromIos() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        try {
            KoinPlatform
                .getKoin()
                .get<SyncManager>()
                .pullRemoteData("00000000-0000-0000-0000-000000000001")
        } catch (e: Exception) {
            println("=== IOS SYNC ERROR: ${e.message}")
            e.printStackTrace()
        }
    }
}

@Suppress("unused") // Called from Swift
fun getDashboardViewModel(userId: String): DashboardViewModel =
    KoinPlatform.getKoin().get<DashboardViewModel> { parametersOf(userId) }

@Suppress("unused") // Called from Swift
fun getChatViewModel(userId: String): ChatViewModel = KoinPlatform.getKoin().get<ChatViewModel> { parametersOf(userId) }

@OptIn(ExperimentalObjCName::class)
@ObjCName(name = "requestHealthPermissionsFromIos")
@Suppress("unused") // Called from Swift
fun requestHealthPermissionsFromIos(onResult: (Boolean) -> Unit) {
    val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    scope.launch {
        try {
            val useCase = KoinPlatform.getKoin().get<SyncHealthMetricsUseCase>()
            val dataReader = KoinPlatform.getKoin().get<HealthDataReader>()
            // Llama a la funcion especifica de iOS
            val result = dataReader.requestPermissionsIos(useCase.requiredPermissions)
            onResult(result)
        } catch (e: Exception) {
            println("=== IOS PERMISSIONS ERROR: ${e.message}")
            onResult(false)
        }
    }
}
