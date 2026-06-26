package dev.tohure.tanayenai.domain.model

data class NotificationSettings(
    val userId: String,
    val morningEnabled: Boolean = false,
    val morningHour: Int = 7,
    val morningMinute: Int = 0,
) {
    init {
        require(morningHour in 0..23) { "morningHour must be 0..23, got $morningHour" }
        require(morningMinute in 0..59) { "morningMinute must be 0..59, got $morningMinute" }
    }
}
