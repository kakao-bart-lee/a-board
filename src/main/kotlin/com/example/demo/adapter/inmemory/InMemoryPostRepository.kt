package com.example.demo.adapter.inmemory

import com.example.demo.domain.model.Post
import com.example.demo.domain.model.Comment
import com.example.demo.domain.port.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.stereotype.Repository

@Repository
class InMemoryPostRepository : PostRepository {
    private val posts = mutableListOf<Post>()

    override suspend fun save(post: Post): Post {
        val idx = posts.indexOfFirst { it.id == post.id }
        if (idx >= 0) {
            posts[idx] = post
        } else {
            posts += post
        }
        return post
    }

    override fun findAll(): Flow<Post> = posts.asFlow()

    override suspend fun findById(id: String): Post? = posts.find { it.id == id }

    override suspend fun addComment(postId: String, comment: Comment, parentCommentId: String?): Comment? {
        val post = findById(postId) ?: return null
        comment.byPostAuthor = comment.authorId == post.authorId
        return if (parentCommentId == null) {
            post.comments += comment
            comment
        } else {
            val parent = post.comments.find { it.id == parentCommentId } ?: return null
            parent.replies += comment
            comment
        }
    }

    override suspend fun incrementViewCount(id: String): Post? {
        val post = findById(id)
        if (post != null && !post.deleted) {
            post.viewCount++
        }
        return post
    }

    override suspend fun deletePost(id: String): Boolean {
        val post = findById(id) ?: return false
        post.deleted = true
        return true
    }

    override suspend fun deleteComment(postId: String, commentId: String, parentCommentId: String?): Boolean {
        val post = findById(postId) ?: return false
        val target = if (parentCommentId == null) {
            post.comments.find { it.id == commentId }
        } else {
            post.comments.find { it.id == parentCommentId }?.replies?.find { it.id == commentId }
        }
        target?.deleted = true
        return target != null
    }
}
