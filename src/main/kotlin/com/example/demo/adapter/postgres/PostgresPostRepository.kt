package com.example.demo.adapter.postgres

import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.domain.port.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactive.asFlow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("prod")
class PostgresPostRepository(
    private val postRepo: PostCrudRepository,
    private val commentRepo: CommentCrudRepository
) : PostRepository {
    override suspend fun save(post: Post): Post {
        val entity = PostEntity(
            id = post.id,
            text = post.text,
            imageUrl = post.imageUrl,
            gender = post.gender,
            authorId = post.authorId,
            anonymousId = post.anonymousId,
            viewCount = post.viewCount,
            deleted = post.deleted,
            reportCount = post.reportCount
        )
        postRepo.save(entity).awaitSingle()
        return post
    }

    override fun findAll(): Flow<Post> = flow {
        postRepo.findAll().asFlow().collect { entity ->
            emit(toDomain(entity))
        }
    }

    override suspend fun findById(id: String): Post? =
        postRepo.findById(id).awaitSingleOrNull()?.let { toDomain(it) }

    override suspend fun addComment(postId: String, comment: Comment, parentCommentId: String?): Comment? {
        val entity = CommentEntity(
            id = comment.id,
            postId = postId,
            authorId = comment.authorId,
            anonymousId = comment.anonymousId,
            text = comment.text,
            parentCommentId = parentCommentId,
            byPostAuthor = comment.byPostAuthor,
            deleted = comment.deleted
        )
        commentRepo.save(entity).awaitSingle()
        return comment
    }

    override suspend fun incrementViewCount(id: String): Post? {
        val updated = postRepo.incrementViewCount(id).awaitSingleOrNull()
        val entity = updated ?: postRepo.findById(id).awaitSingleOrNull() ?: return null
        return toDomain(entity)
    }

    override suspend fun deletePost(id: String): Boolean {
        val post = postRepo.findById(id).awaitSingleOrNull() ?: return false
        post.deleted = true
        postRepo.save(post).awaitSingle()
        return true
    }

    override suspend fun deleteComment(postId: String, commentId: String, parentCommentId: String?): Boolean {
        val comment = commentRepo.findById(commentId).awaitSingleOrNull() ?: return false
        comment.deleted = true
        commentRepo.save(comment).awaitSingle()
        return true
    }

    override fun findReported(): Flow<Post> = flow {
        postRepo.findAll().asFlow().collect { entity ->
            if (entity.reportCount > 0 && !entity.deleted) emit(toDomain(entity))
        }
    }

    override fun findByAuthorId(authorId: String): Flow<Post> = flow {
        postRepo.findByAuthorId(authorId).asFlow().collect { entity ->
            emit(toDomain(entity))
        }
    }

    override suspend fun reportPost(id: String): Post? {
        val post = postRepo.findById(id).awaitSingleOrNull() ?: return null
        post.reportCount++
        postRepo.save(post).awaitSingle()
        return toDomain(post)
    }

    override suspend fun moderatePost(id: String, delete: Boolean): Post? {
        val post = postRepo.findById(id).awaitSingleOrNull() ?: return null
        if (delete) post.deleted = true
        post.reportCount = 0
        postRepo.save(post).awaitSingle()
        return toDomain(post)
    }

    private suspend fun toDomain(entity: PostEntity): Post {
        val comments = commentRepo.findByPostId(entity.id!!).collectList().awaitSingle()
        val domainComments = comments.filter { it.parentCommentId == null }.map { ce ->
            toDomainComment(ce, comments)
        }.toMutableList()
        return Post(
            id = entity.id!!,
            text = entity.text,
            imageUrl = entity.imageUrl,
            gender = entity.gender,
            authorId = entity.authorId,
            anonymousId = entity.anonymousId,
            comments = domainComments,
            viewCount = entity.viewCount,
            deleted = entity.deleted,
            reportCount = entity.reportCount,
            canDelete = false
        )
    }

    private fun toDomainComment(entity: CommentEntity, all: List<CommentEntity>): Comment {
        val replies = all.filter { it.parentCommentId == entity.id }.map { toDomainComment(it, all) }.toMutableList()
        return Comment(
            id = entity.id!!,
            postId = entity.postId,
            authorId = entity.authorId,
            anonymousId = entity.anonymousId,
            text = entity.text,
            parentCommentId = entity.parentCommentId,
            replies = replies,
            byPostAuthor = entity.byPostAuthor,
            deleted = entity.deleted,
            canDelete = false
        )
    }
}
