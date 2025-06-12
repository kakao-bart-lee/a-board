package com.example.demo.application

import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.domain.port.PostRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class PostService(private val repository: PostRepository) {

    suspend fun createPost(text: String, imageUrl: String?, gender: String?, authorId: String): Post {
        val post = Post(text = text, imageUrl = imageUrl, gender = gender, authorId = authorId)
        return repository.save(post)
    }

    fun getPosts(): Flow<Post> = repository.findAll()

    suspend fun getPost(id: String): Post? = repository.incrementViewCount(id)

    suspend fun addComment(postId: String, text: String, authorId: String, parentCommentId: String? = null): Comment? {
        val comment = Comment(postId = postId, authorId = authorId, text = text, parentCommentId = parentCommentId)
        return repository.addComment(postId, comment, parentCommentId)
    }

    suspend fun deletePost(id: String): Boolean = repository.deletePost(id)

    suspend fun deleteComment(postId: String, commentId: String, parentCommentId: String? = null): Boolean =
        repository.deleteComment(postId, commentId, parentCommentId)
}
