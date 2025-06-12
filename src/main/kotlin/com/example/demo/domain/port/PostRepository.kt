package com.example.demo.domain.port

import com.example.demo.domain.model.Post
import com.example.demo.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun save(post: Post): Post
    fun findAll(): Flow<Post>
    suspend fun findById(id: String): Post?
    suspend fun addComment(postId: String, comment: Comment, parentCommentId: String? = null): Comment?
    suspend fun incrementViewCount(id: String): Post?
    suspend fun deletePost(id: String): Boolean
    suspend fun deleteComment(postId: String, commentId: String, parentCommentId: String? = null): Boolean
    fun findReported(): Flow<Post>
    fun findByAuthorId(authorId: String): Flow<Post>
    suspend fun reportPost(id: String): Post?
    suspend fun moderatePost(id: String, delete: Boolean): Post?
}
