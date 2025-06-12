package com.example.demo.adapter.postgres

import com.example.demo.domain.model.User
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Repository

@Repository
class PostgresUserRepository(private val repo: UserCrudRepository) : UserRepository {
    override suspend fun save(user: User): User {
        val entity = UserEntity(
            id = user.id,
            name = user.name,
            gender = user.gender,
            birthYear = user.birthYear,
            profileImageUrls = user.profileImageUrls.joinToString(","),
            location = user.location,
            preferredLanguage = user.preferredLanguage,
            aboutMe = user.aboutMe,
            role = user.role
        )
        repo.save(entity).awaitSingle()
        return user
    }

    override fun findAll(): Flow<User> =
        repo.findAll().map { toDomain(it) }.asFlow()

    override suspend fun findById(id: String): User? =
        repo.findById(id).map { toDomain(it) }.awaitSingleOrNull()

    private fun toDomain(entity: UserEntity) = User(
        id = entity.id!!,
        name = entity.name,
        gender = entity.gender,
        birthYear = entity.birthYear,
        profileImageUrls = if (entity.profileImageUrls.isBlank()) emptyList() else entity.profileImageUrls.split(","),
        location = entity.location,
        preferredLanguage = entity.preferredLanguage,
        aboutMe = entity.aboutMe,
        role = entity.role
    )
}
