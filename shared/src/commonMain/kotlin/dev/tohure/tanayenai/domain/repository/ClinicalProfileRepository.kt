package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.ClinicalProfile

interface ClinicalProfileRepository {
    suspend fun getClinicalProfile(userId: String): ClinicalProfile?

    suspend fun saveClinicalProfile(profile: ClinicalProfile)
}
