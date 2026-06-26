package dev.tohure.tanayenai.data.repository

import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.ActivityLevel
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.Sex
import dev.tohure.tanayenai.domain.model.User
import dev.tohure.tanayenai.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import db.UserProfile as DbUserProfile

class UserRepositoryImpl(
    private val database: TanayenDatabase,
) : UserRepository {
    private val queries = database.userProfileQueries

    override suspend fun getUser(id: String): User? =
        withContext(Dispatchers.Default) {
            queries.getUser(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun saveUser(user: User): Unit =
        withContext(Dispatchers.Default) {
            queries.insertUser(
                id = user.id,
                name = user.name,
                birthDate = user.birthDate,
                sex = user.sex.name,
                heightCm = user.heightCm.toDouble(),
                goal = user.goal.name,
                activityLevel = user.activityLevel.name,
            )
        }

    override suspend fun updateUser(user: User): Unit =
        withContext(Dispatchers.Default) {
            queries.updateUser(
                name = user.name,
                birthDate = user.birthDate,
                sex = user.sex.name,
                heightCm = user.heightCm.toDouble(),
                goal = user.goal.name,
                activityLevel = user.activityLevel.name,
                id = user.id,
            )
        }

    private fun DbUserProfile.toDomain() =
        User(
            id = id,
            name = name,
            birthDate = birth_date,
            sex = Sex.valueOf(sex),
            heightCm = height_cm.toFloat(),
            goal = NutritionGoal.valueOf(goal),
            activityLevel = ActivityLevel.valueOf(activity_level),
        )
}
