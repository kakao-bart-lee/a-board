package com.example.demo.application

import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.domain.port.PostRepository
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import org.springframework.stereotype.Service

@Service
class PostService(
    private val repository: PostRepository,
    private val userRepository: UserRepository,
) {

    suspend fun createPost(
        text: String,
        imageUrl: String?,
        gender: String?,
        authorId: String,
        anonymousId: String
    ): Post {
        if (isSuspended(authorId)) throw IllegalStateException("user suspended")
        val post = Post(text = text, imageUrl = imageUrl, gender = gender, authorId = authorId, anonymousId = anonymousId)
        return repository.save(post)
    }

    fun getPosts(offset: Int = 0, limit: Int? = null): Flow<Post> {
        var flow = repository.findAll()
        if (offset > 0) flow = flow.drop(offset)
        if (limit != null) flow = flow.take(limit)
        return flow
    }

    fun getPostsByUser(userId: String): Flow<Post> = repository.findByAuthorId(userId)

    fun getReportedPosts(): Flow<Post> = repository.findReported()

    suspend fun getPost(id: String): Post? = repository.incrementViewCount(id)

    suspend fun updatePost(
        id: String,
        text: String?,
        imageUrl: String?,
        gender: String?,
        requesterId: String,
        admin: Boolean
    ): Post? {
        val existing = repository.findById(id) ?: return null
        if (!admin && existing.authorId != requesterId) return null
        val updated = existing.copy(
            text = text ?: existing.text,
            imageUrl = imageUrl ?: existing.imageUrl,
            gender = gender ?: existing.gender
        )
        return repository.save(updated)
    }

    suspend fun addComment(
        postId: String,
        text: String,
        authorId: String,
        anonymousId: String,
        parentCommentId: String? = null
    ): Comment? {
        if (isSuspended(authorId)) throw IllegalStateException("user suspended")
        val comment = Comment(
            postId = postId,
            authorId = authorId,
            anonymousId = anonymousId,
            text = text,
            parentCommentId = parentCommentId
        )
        return repository.addComment(postId, comment, parentCommentId)
    }

    suspend fun deletePost(id: String, requesterId: String, admin: Boolean): Boolean {
        val post = repository.findById(id) ?: return false
        return if (admin || post.authorId == requesterId) {
            repository.deletePost(id)
        } else {
            false
        }
    }

    suspend fun deleteComment(
        postId: String,
        commentId: String,
        requesterId: String,
        admin: Boolean,
        parentCommentId: String? = null
    ): Boolean {
        val post = repository.findById(postId) ?: return false
        val targetAuthor = findCommentAuthor(post, commentId, parentCommentId)
        return if (admin || targetAuthor == requesterId) {
            repository.deleteComment(postId, commentId, parentCommentId)
        } else {
            false
        }
    }

    private fun findCommentAuthor(post: Post, commentId: String, parentCommentId: String?): String? {
        return if (parentCommentId == null) {
            post.comments.find { it.id == commentId }?.authorId
        } else {
            post.comments.find { it.id == parentCommentId }?.replies?.find { it.id == commentId }?.authorId
        }
    }

    suspend fun reportPost(id: String): Post? = repository.reportPost(id)

    suspend fun moderatePost(id: String, delete: Boolean, moderator: Boolean): Post? {
        if (!moderator) return null
        return repository.moderatePost(id, delete)
    }

    private suspend fun isSuspended(userId: String): Boolean {
        val user = userRepository.findById(userId) ?: return false
        val until = user.suspendedUntil ?: return false
        return java.time.Instant.now().isBefore(until)
    }
}
