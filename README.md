# 🌿 Tanayen AI

**Asistente nutricional inteligente impulsado por Gemini AI**
_Tus datos de salud, tus metas, tus recomendaciones — todo en un solo lugar._

[![Android Build](https://img.shields.io/github/actions/workflow/status/tohure/TanayenAI/ci.yml?branch=develop&label=Android%20Build&logo=android&color=3DDC84)](https://github.com/tohure/TanayenAI/actions/workflows/ci.yml)
[![iOS Build](https://img.shields.io/github/actions/workflow/status/tohure/TanayenAI/ci.yml?branch=develop&label=iOS%20Build&logo=apple&color=007AFF)](https://github.com/tohure/TanayenAI/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![KMP](https://img.shields.io/badge/Kotlin%20Multiplatform-iOS%20%7C%20Android-orange?logo=kotlin)](https://www.jetbrains.com/kotlin-multiplatform/)
[![Gemini](https://img.shields.io/badge/Gemini%202.5%20Flash-AI%20Powered-blue?logo=google&logoColor=white)](https://ai.google.dev/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## 📱 Sobre la App

Tanayen AI es una aplicación **Kotlin Multiplatform (KMP)** para Android e iOS que usa inteligencia artificial para darte recomendaciones nutricionales personalizadas basadas en tus métricas de salud reales, alacena disponible y perfil clínico.

> ⚠️ **Esta app no reemplaza a un médico o profesional de la salud.**

---

## ✨ Funcionalidades

- **Chat inteligente con Gemini** — conversación en lenguaje natural con contexto clínico inyectado en cada turno
- **Imágenes en el chat** — toma o adjunta fotos directamente desde el chat; Gemini determina el contexto automáticamente:
  - 🛒 Comparación de productos en el supermercado
  - 🍽️ Recomendaciones de platos en menús de restaurante
  - 🏠 Detección de ingredientes domésticos para guardar en la alacena
- **Alacena inteligente** — Gemini sugiere ingredientes detectados en imágenes; el usuario confirma antes de guardar
- **Sincronización de salud** — métricas reales desde Health Connect (Android) y HealthKit (iOS)
- **Perfil clínico** — restricciones dietéticas basadas en colesterol, glucosa, presión arterial y más
- **Historial de recomendaciones** — Gemini evita repetir sugerencias recientes

---

## 🛠️ Stack Técnico

| Capa | Tecnología |
|---|---|
| **Multiplataforma** | Kotlin Multiplatform (KMP) |
| **Android UI** | Jetpack Compose |
| **iOS UI** | SwiftUI |
| **AI** | Google Gemini 2.5 Flash (SDK KMP) |
| **Salud Android** | Health Connect |
| **Salud iOS** | HealthKit |
| **Backend / Auth** | Supabase (PostgreSQL + Auth + Realtime) |
| **Base de datos local** | SQLDelight |
| **Networking** | Ktor Client |
| **Inyección de dependencias** | Koin |
| **Coroutines iOS** | KMP-NativeCoroutines |
| **Build System** | Gradle 9, AGP 9 |
| **CI/CD** | GitHub Actions + Firebase App Distribution |

---

## 🏗️ Arquitectura

El proyecto sigue una arquitectura **Clean Architecture + MVVM** con código compartido en KMP.

```
shared/
├── commonMain/
│   ├── data/          # Repositorios, datasources remotos/locales, sync
│   ├── domain/
│   │   ├── model/     # Entidades de dominio
│   │   ├── repository/# Interfaces de repositorio
│   │   └── usecase/   # Casos de uso (lógica de negocio)
│   ├── presentation/
│   │   └── viewmodel/ # ViewModels compartidos Android/iOS
│   └── di/            # Módulos Koin
├── androidMain/       # Implementaciones Android (HealthConnect, DB driver)
└── iosMain/           # Implementaciones iOS (HealthKit, DB driver, Koin init)
```

### Flujo de imágenes en el chat

```
Usuario adjunta imagen (cámara/galería)
        ↓
Compresión y conversión a Base64 en capa nativa (Android/iOS)
        ↓
ChatViewModel envía imagen + texto + contexto clínico a Gemini
        ↓
Gemini responde en streaming — ChatTagParser oculta tags parciales en tiempo real
        ↓
Al cerrar el stream: se detectan tags [PANTRY:...] y [REC:...]
        ↓
Si hay [PANTRY:...] → chip de confirmación en el chat
        ↓
Usuario confirma → SavePantryIngredientsUseCase guarda sin duplicados
```

### Tags de sistema (protocolo con Gemini)

El sistema usa un protocolo de tags al final de cada respuesta, invisible para el usuario:

```
[PANTRY:ingrediente1|ingrediente2|...]     ← ingredientes para guardar en alacena
[REC:TIPO:título:ingrediente1|ingrediente2|...]  ← recomendación para guardar en historial
```

El separador `|` (pipe) evita ambigüedad con nombres compuestos como `sal, pimienta`.

---

## 🧪 Tests

```
shared/src/commonTest/
├── domain/usecase/
│   ├── BuildContextUseCaseTest   # Construcción de contexto clínico para Gemini
│   ├── ChatTagParserTest         # Parsing y limpieza de tags en streaming
│   ├── FetchContextParamsUseCaseTest  # Obtención de datos para el contexto
│   └── SavePantryIngredientsUseCaseTest  # Deduplicación de ingredientes
shared/src/androidHostTest/
└── data/repository/
    └── PantryRepositoryIntegrationTest  # CRUD con SQLDelight real
```

```bash
# Ejecutar todos los tests
./gradlew :shared:allTests
```

---

## 🚀 Puesta en marcha

### Requisitos

- Android Studio Meerkat o superior
- Xcode 16+
- JDK 21+ (el CI usa Zulu 21)

### Configuración

1. Clona el repo
2. Copia tus credenciales en `local.properties`:
   ```properties
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_ANON_KEY=tu_anon_key
   GEMINI_API_KEY=tu_api_key
   ```
3. Para iOS, agrega las mismas variables en `iosApp/Configuration/Secrets.xcconfig`:
   ```
   SUPABASE_URL = https://tu-proyecto.supabase.co
   SUPABASE_ANON_KEY = tu_anon_key
   GEMINI_API_KEY = tu_api_key
   ```

### Ejecutar

```bash
# Android
./gradlew :androidApp:assembleDebug

# Shared (compilar + tests)
./gradlew :shared:build
```

Para iOS: abre `iosApp/iosApp.xcodeproj` en Xcode y ejecuta en simulador o dispositivo.

---

## 🔄 CI/CD

GitHub Actions (`.github/workflows/ci.yml`) automatiza lint, tests, builds y distribución:

| Evento | Qué corre |
|---|---|
| **PR a `main`/`develop`** | ktlint + tests, build APK Android, build de simulador iOS (solo compila) |
| **Push a `develop`** | APK debug → **Firebase App Distribution** (grupo `testers`) |
| **Push a `main`** | AAB release firmado → **Firebase App Distribution** (grupo `release-testers`) |

> 📦 **La distribución es solo Android** (vía Firebase App Distribution). El build de iOS en CI únicamente compila el simulador para detectar errores; no hay publicación en TestFlight/App Store.

---

## 👤 Autor

[![GitHub](https://img.shields.io/badge/GitHub-tohure-181717?logo=github&logoColor=white)](https://github.com/tohure)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Carlo%20Huaman-0A66C2?logo=linkedin&logoColor=white)](https://linkedin.com/in/tohure)
[![X](https://img.shields.io/badge/X-@tohure__-000000?logo=x&logoColor=white)](https://x.com/tohure_)
