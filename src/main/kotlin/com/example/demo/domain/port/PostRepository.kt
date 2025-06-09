package com.example.demo.domain.port

import com.example.demo.domain.model.Post
import com.example.demo.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun save(post: Post): Post
    fun findAll(): Flow<Post>
    suspend fun findById(id: String): Post?
    suspend fun addComment(postId: String, comment: Comment, parentCommentId: String? = null): Comment?
}
