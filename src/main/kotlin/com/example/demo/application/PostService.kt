package com.example.demo.application

import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.domain.port.PostRepository
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service

/**
 * Service containing the main business logic for posts and comments.
 * It validates authorship, handles moderation and applies deletion flags
 * so controllers can remain thin.
 */
@Service
class PostService(
    private val repository: PostRepository,
    private val userRepository: UserRepository,
) {

    /**
     * Create a new post on behalf of a user.
     *
     * @param text post body text
     * @param imageUrl optional image URL
     * @param gender optional gender hint displayed with the post
     * @param authorId actual user id of the author
     * @param anonymousId random id from the JWT to keep the author anonymous
     */
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

    fun getPosts(offset: Int = 0, limit: Int? = null, requesterId: String? = null): Flow<Post> {
        var flow = repository.findAll()
        if (offset > 0) flow = flow.drop(offset)
        if (limit != null) flow = flow.take(limit)
        if (requesterId != null) {
            flow = flow.map { applyDeletionFlags(it, requesterId); it }
        }
        return flow
    }

    fun getPostsByUser(userId: String, requesterId: String? = null): Flow<Post> {
        var flow = repository.findByAuthorId(userId)
        if (requesterId != null) {
            flow = flow.map { applyDeletionFlags(it, requesterId); it }
        }
        return flow
    }

    fun getReportedPosts(requesterId: String? = null): Flow<Post> {
        var flow = repository.findReported()
        if (requesterId != null) {
            flow = flow.map { applyDeletionFlags(it, requesterId); it }
        }
        return flow
    }

    suspend fun getPost(id: String, requesterId: String? = null): Post? {
        val post = repository.incrementViewCount(id)
        if (post != null && requesterId != null) applyDeletionFlags(post, requesterId)
        return post
    }

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

    private fun applyDeletionFlags(post: Post, requesterId: String) {
        post.canDelete = post.authorId == requesterId
        post.comments.forEach { applyDeletionFlags(it, requesterId) }
    }

    private fun applyDeletionFlags(comment: Comment, requesterId: String) {
        comment.canDelete = comment.authorId == requesterId
        comment.replies.forEach { applyDeletionFlags(it, requesterId) }
    }

    /**
     * Check whether a user is currently suspended.
     * A suspended user cannot create posts or comments.
     */
    private suspend fun isSuspended(userId: String): Boolean {
        val user = userRepository.findById(userId) ?: return false
        val until = user.suspendedUntil ?: return false
        return java.time.Instant.now().isBefore(until)
    }
}
