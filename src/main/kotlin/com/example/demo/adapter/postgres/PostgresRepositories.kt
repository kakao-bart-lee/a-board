package com.example.demo.adapter.postgres

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.r2dbc.repository.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface PostCrudRepository : R2dbcRepository<PostEntity, String> {
    fun findByAuthorId(authorId: String): Flux<PostEntity>

    @Query("UPDATE posts SET view_count = view_count + 1 WHERE id = :id AND deleted = false RETURNING *")
    fun incrementViewCount(id: String): Mono<PostEntity>
}
interface CommentCrudRepository : R2dbcRepository<CommentEntity, String> {
    fun findByPostId(postId: String): Flux<CommentEntity>
}
interface UserCrudRepository : R2dbcRepository<UserEntity, String> {
    fun findByEmail(email: String): Mono<UserEntity>
}
