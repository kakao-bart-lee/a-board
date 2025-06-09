package com.example.demo.service

import com.example.demo.model.Comment
import com.example.demo.model.Post
import org.springframework.stereotype.Service

@Service
class PostService {
    private val posts = mutableListOf<Post>()

    fun createPost(text: String, imageUrl: String?, gender: String?): Post {
        val post = Post(text = text, imageUrl = imageUrl, gender = gender)
        posts += post
        return post
    }

    fun getPosts(): List<Post> = posts

    fun getPost(id: String): Post? = posts.find { it.id == id }

    fun addComment(postId: String, text: String): Comment? {
        val post = getPost(postId) ?: return null
        val comment = Comment(postId = postId, text = text)
        post.comments += comment
        return comment
    }
}
