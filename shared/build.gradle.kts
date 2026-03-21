import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kmp.nativecoroutines)
}

kotlin {
    android {
        namespace = "dev.tohure.tanayenai.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }

        withHostTestBuilder {}.configure {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "dev.tohure.tanayenai.shared")
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }

        commonMain.dependencies {
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            // Supabase
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)

            // AI (Gemini)
            implementation(libs.generativeai.google)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)

            // Lifecycle ViewModel (KMP)
            implementation(libs.androidx.lifecycle.viewmodel)

            // Logging
            implementation(libs.kermit)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.health.connect)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.kotlin.testJunit)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

sqldelight {
    databases {
        create("TanayenDatabase") {
            packageName.set("dev.tohure.tanayenai.db")
            dialect(libs.sqldelight.dialect.sqlite)
        }
    }
}

// Ktlint task para CI
tasks.register<Exec>("ktlintCheck") {
    group = "verification"
    description = "Runs ktlint on Kotlin source files"
    commandLine("ktlint", "--editorconfig=../.editorconfig", "src/**/*.kt")
    workingDir(projectDir)
}
