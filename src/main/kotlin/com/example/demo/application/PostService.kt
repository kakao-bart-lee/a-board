package com.example.demo.application

import com.example.demo.domain.model.Attachment
import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Notification
import com.example.demo.domain.model.Post
import com.example.demo.domain.port.FileStoragePort
import com.example.demo.domain.port.NotificationRepository
import com.example.demo.domain.port.PostRepository
import com.example.demo.domain.port.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
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
    private val notificationRepository: NotificationRepository,
    private val fileStoragePort: FileStoragePort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Create a new post on behalf of a user.
     *
     * @param text post body text
     * @param attachments list of attachments
     * @param gender optional gender hint displayed with the post
     * @param authorId actual user id of the author
     * @param anonymousId random id from the JWT to keep the author anonymous
     */
    suspend fun createPost(
        text: String,
        attachments: List<Attachment>,
        gender: String?,
        authorId: String,
        anonymousId: String
    ): Post {
        log.info("Creating post for user $authorId")
        if (isSuspended(authorId)) {
            log.warn("User $authorId is suspended, cannot create post")
            throw IllegalStateException("user suspended")
        }
        val post = Post(text = text, attachments = attachments, gender = gender, authorId = authorId, anonymousId = anonymousId)
        return repository.save(post)
    }

    fun getPosts(offset: Int = 0, limit: Int? = null, requesterId: String? = null): Flow<Post> {
        log.info("Getting posts with offset $offset, limit $limit")
        var flow = repository.findAll()
        if (offset > 0) flow = flow.drop(offset)
        if (limit != null) flow = flow.take(limit)
        if (requesterId != null) {
            flow = flow.map { applyDeletionFlags(it, requesterId); it }
        }
        return flow
    }

    fun getPostsByUser(userId: String, requesterId: String? = null): Flow<Post> {
        log.info("Getting posts for user $userId")
        var flow = repository.findByAuthorId(userId)
        if (requesterId != null) {
            flow = flow.map { applyDeletionFlags(it, requesterId); it }
        }
        return flow
    }

    fun getReportedPosts(requesterId: String? = null): Flow<Post> {
        log.info("Getting reported posts")
        var flow = repository.findReported()
        if (requesterId != null) {
            flow = flow.map { applyDeletionFlags(it, requesterId); it }
        }
        return flow
    }

    suspend fun getPost(id: String, requesterId: String? = null): Post? {
        log.info("Getting post $id")
        val post = repository.incrementViewCount(id)
        if (post != null && requesterId != null) applyDeletionFlags(post, requesterId)
        return post
    }

    suspend fun updatePost(
        id: String,
        text: String?,
        attachments: List<Attachment>?,
        gender: String?,
        requesterId: String,
        admin: Boolean
    ): Post? {
        log.info("Updating post $id by user $requesterId (admin: $admin)")
        val existing = repository.findById(id) ?: return null
        if (!admin && existing.authorId != requesterId) {
            log.warn("User $requesterId is not authorized to update post $id")
            return null
        }
        val updated = existing.copy(
            text = text ?: existing.text,
            attachments = attachments ?: existing.attachments,
            gender = gender ?: existing.gender
        )
        return repository.save(updated)
    }

    suspend fun addComment(
        postId: String,
        text: String,
        attachments: List<Attachment>,
        authorId: String,
        anonymousId: String,
        parentCommentId: String? = null
    ): Comment? {
        log.info("Adding comment to post $postId by user $authorId")
        if (isSuspended(authorId)) {
            log.warn("User $authorId is suspended, cannot add comment")
            throw IllegalStateException("user suspended")
        }
        val post = repository.findById(postId) ?: return null
        val comment = Comment(
            postId = postId,
            authorId = authorId,
            anonymousId = anonymousId,
            text = text,
            attachments = attachments,
            parentCommentId = parentCommentId
        )
        val savedComment = repository.addComment(postId, comment, parentCommentId)

        if (savedComment != null) {
            val targetUserId = if (parentCommentId == null) {
                post.authorId
            } else {
                post.comments.find { it.id == parentCommentId }?.authorId
            }

            if (targetUserId != null && targetUserId != authorId) {
                val notification = Notification(
                    userId = targetUserId,
                    sourcePostId = postId,
                    sourceCommentId = parentCommentId,
                    triggeringAnonymousId = anonymousId,
                    message = "A new reply was added to your post/comment."
                )
                notificationRepository.save(notification)
            }
        }
        return savedComment
    }

    suspend fun deletePost(id: String, requesterId: String, admin: Boolean): Boolean {
        log.info("Deleting post $id by user $requesterId (admin: $admin)")
        val post = repository.findById(id) ?: return false
        return if (admin || post.authorId == requesterId) {
            repository.deletePost(id)
        } else {
            log.warn("User $requesterId is not authorized to delete post $id")
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
        log.info("Deleting comment $commentId from post $postId by user $requesterId (admin: $admin)")
        val post = repository.findById(postId) ?: return false
        val targetAuthor = findCommentAuthor(post, commentId, parentCommentId)
        return if (admin || targetAuthor == requesterId) {
            repository.deleteComment(postId, commentId, parentCommentId)
        } else {
            log.warn("User $requesterId is not authorized to delete comment $commentId")
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

    suspend fun reportPost(id: String): Post? {
        log.info("Reporting post $id")
        return repository.reportPost(id)
    }

    suspend fun moderatePost(id: String, delete: Boolean, moderator: Boolean): Post? {
        log.info("Moderating post $id (delete: $delete)")
        if (!moderator) {
            log.warn("User is not a moderator, cannot moderate post $id")
            return null
        }
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
