package dev.tohure.tanayenai

class Greeting {
    private val platform = getPlatform()

    fun greet() = "Hello, ${platform.name}!"
}
