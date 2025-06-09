package com.example.demo.application

import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.domain.port.PostRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class PostService(private val repository: PostRepository) {

    suspend fun createPost(text: String, imageUrl: String?, gender: String?): Post {
        val post = Post(text = text, imageUrl = imageUrl, gender = gender)
        return repository.save(post)
    }

    fun getPosts(): Flow<Post> = repository.findAll()

    suspend fun getPost(id: String): Post? = repository.findById(id)

    suspend fun addComment(postId: String, text: String): Comment? {
        val comment = Comment(postId = postId, text = text)
        return repository.addComment(postId, comment)
    }
}
