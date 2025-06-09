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
        posts += post
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
}
