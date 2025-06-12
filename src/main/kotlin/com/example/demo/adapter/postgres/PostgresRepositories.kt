package com.example.demo.adapter.postgres

import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux

interface PostCrudRepository : R2dbcRepository<PostEntity, String>
interface CommentCrudRepository : R2dbcRepository<CommentEntity, String> {
    fun findByPostId(postId: String): Flux<CommentEntity>
}
interface UserCrudRepository : R2dbcRepository<UserEntity, String>
