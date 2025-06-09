package com.example.demo.service

import com.example.demo.model.Comment
import com.example.demo.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.stereotype.Service

@Service
class PostService {
    private val posts = mutableListOf<Post>()

    suspend fun createPost(text: String, imageUrl: String?, gender: String?): Post {
        val post = Post(text = text, imageUrl = imageUrl, gender = gender)
        posts += post
        return post
    }

    fun getPosts(): Flow<Post> = posts.asFlow()

    suspend fun getPost(id: String): Post? = posts.find { it.id == id }

    suspend fun addComment(postId: String, text: String): Comment? {
        val post = getPost(postId) ?: return null
        val comment = Comment(postId = postId, text = text)
        post.comments += comment
        return comment
    }
}
