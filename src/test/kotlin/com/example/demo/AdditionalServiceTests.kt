package com.example.demo

import com.example.demo.adapter.inmemory.InMemoryPostRepository
import com.example.demo.adapter.inmemory.InMemoryUserRepository
import com.example.demo.application.PostService
import com.example.demo.application.UserService
import com.example.demo.config.JwtService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AdditionalServiceTests {
    private val postRepo = InMemoryPostRepository()
    private val postService = PostService(postRepo)
    private val userRepo = InMemoryUserRepository()
    private val userService = UserService(userRepo)
    private val jwtService = JwtService()

    @Test
    fun `comment by post author flagged`() = runBlocking {
        val post = postService.createPost("hello", null, null, "u1", "anon1")
        val comment = postService.addComment(post.id, "author comment", "u1", "anon1")!!
        assertTrue(comment.byPostAuthor)
    }

    @Test
    fun `nested replies are stored`() = runBlocking {
        val post = postService.createPost("hello", null, null, "u1", "anon1")
        val parent = postService.addComment(post.id, "parent", "u2", "anon2")!!
        val reply = postService.addComment(post.id, "child", "u3", "anon3", parent.id)!!
        val saved = postRepo.findById(post.id)!!
        assertEquals(1, saved.comments.find { it.id == parent.id }?.replies?.size)
        assertEquals(reply.id, saved.comments.find { it.id == parent.id }?.replies?.first()?.id)
    }

    @Test
    fun `unauthorized user cannot delete comment`() = runBlocking {
        val post = postService.createPost("hello", null, null, "u1", "anon1")
        val comment = postService.addComment(post.id, "c", "u1", "anon1")!!
        val result = postService.deleteComment(post.id, comment.id, "u2", false)
        assertFalse(result)
        assertFalse(comment.deleted)
    }

    @Test
    fun `user service creates and fetches users`() = runBlocking {
        val user = userService.createUser("name", "M", 1990, listOf("img"), "loc", "en", "hi")
        val fetched = userService.getUser(user.id)
        assertEquals(user.name, fetched?.name)
        assertEquals("loc", fetched?.location)
    }

    @Test
    fun `jwt tokens verify`() {
        val token = jwtService.generateToken("u1", "ADMIN", "anon")
        val decoded = jwtService.verify(token)
        assertNotNull(decoded)
        assertEquals("u1", decoded!!.subject)
        assertEquals("ADMIN", decoded.getClaim("role").asString())
        assertEquals("anon", decoded.getClaim("anon").asString())
    }
}
