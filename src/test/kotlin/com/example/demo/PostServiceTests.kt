package com.example.demo

import com.example.demo.adapter.inmemory.InMemoryPostRepository
import com.example.demo.application.PostService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PostServiceTests {
    private val repository = InMemoryPostRepository()
    private val service = PostService(repository)

    @Test
    fun `viewing a post increases view count`() = runBlocking {
        val post = service.createPost("hello", null, null, "u1", "anon1")
        val fetched = service.getPost(post.id)
        assertEquals(1, fetched?.viewCount)
    }

    @Test
    fun `delete marks post and comment`() = runBlocking {
        val post = service.createPost("hello", null, null, "u1", "anon1")
        val comment = service.addComment(post.id, "hi", "u2", "anon2")!!
        val child = service.addComment(post.id, "reply", "u3", "anon3", comment.id)!!
        service.deleteComment(post.id, child.id, "u3", true, comment.id)
        assertTrue(comment.replies[0].deleted)
        service.deletePost(post.id, "u1", true)
        val deleted = repository.findById(post.id)!!
        assertTrue(deleted.deleted)
    }
}
