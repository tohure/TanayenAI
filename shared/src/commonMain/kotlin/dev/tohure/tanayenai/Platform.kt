package dev.tohure.tanayenai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
