# 🌿 Tanayen AI

**Asistente nutricional inteligente impulsado por Gemini AI**
_Tus datos de salud, tus metas, tus recomendaciones — todo en un solo lugar._

[![Android Build](https://img.shields.io/github/actions/workflow/status/tohure/TanayenAI/android.yml?branch=develop&label=Android%20Build&logo=android&color=3DDC84)](https://github.com/tohure/TanayenAI/actions)
[![iOS Build](https://img.shields.io/github/actions/workflow/status/tohure/TanayenAI/ios.yml?branch=develop&label=iOS%20Build&logo=apple&color=007AFF)](https://github.com/tohure/TanayenAI/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![KMP](https://img.shields.io/badge/Kotlin%20Multiplatform-iOS%20%7C%20Android-orange?logo=kotlin)](https://www.jetbrains.com/kotlin-multiplatform/)
[![Gemini](https://img.shields.io/badge/Gemini%202.5%20Flash-AI%20Powered-blue?logo=google&logoColor=white)](https://ai.google.dev/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)


---

## 📱 Sobre la App

Tanayen AI es una aplicación **Kotlin Multiplatform (KMP)** para Android e iOS que usa inteligencia artificial para darte recomendaciones nutricionales personalizadas basadas en tus métricas de salud reales.

> ⚠️ **Esta app no reemplaza a un médico o profesional de la salud.**

---

## 🛠️ Stack Técnico

| Capa | Tecnología |
|---|---|
| **Multiplataforma** | Kotlin Multiplatform (KMP) |
| **Android UI** | Jetpack Compose |
| **iOS UI** | SwiftUI |
| **AI** | Google Gemini 2.5 Flash (SDK KMP) |
| **Backend / Auth** | Supabase (PostgreSQL + Auth + Realtime) |
| **Base de datos local** | SQLDelight |
| **Networking** | Ktor Client |
| **Inyección de dependencias** | Koin |
| **Build System** | Gradle 9, AGP 9 |

---

## 🚀 Puesta en marcha

1. Clona el repo
2. Copia tus credenciales en `local.properties`:
   ```properties
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_ANON_KEY=tu_anon_key
   GEMINI_API_KEY=tu_api_key
   ```
3. Para iOS, agrega las mismas variables en `iosApp/Configuration/Secrets.xcconfig`
4. Android: `./gradlew :androidApp:assembleDebug`
5. iOS: Abre `iosApp/iosApp.xcworkspace` en Xcode y ejecuta

---


## 👤 Autor

[![GitHub](https://img.shields.io/badge/GitHub-tohure-181717?logo=github&logoColor=white)](https://github.com/tohure)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Carlo%20Huaman-0A66C2?logo=linkedin&logoColor=white)](https://linkedin.com/in/tohure)
[![X](https://img.shields.io/badge/X-@tohure__-000000?logo=x&logoColor=white)](https://x.com/tohure_)
