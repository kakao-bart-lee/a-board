package com.example.demo

import com.example.demo.adapter.inmemory.InMemoryPostRepository
import com.example.demo.application.PostService
import com.example.demo.adapter.inmemory.InMemoryUserRepository
import com.example.demo.domain.model.User
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PostServiceTests {
    private val repository = InMemoryPostRepository()
    private val userRepo = InMemoryUserRepository()
    private val service = PostService(repository, userRepo)

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

    @Test
    fun `author can edit post`() = runBlocking {
        val post = service.createPost("hello", null, null, "u1", "anon1")
        val updated = service.updatePost(post.id, "bye", null, null, "u1", false)
        assertEquals("bye", updated?.text)
    }

    @Test
    fun `non author cannot edit post`() = runBlocking {
        val post = service.createPost("hello", null, null, "u1", "anon1")
        val updated = service.updatePost(post.id, "bye", null, null, "u2", false)
        assertTrue(updated == null)
    }

    @Test
    fun `report and moderate`() = runBlocking {
        val post = service.createPost("hello", null, null, "u1", "anon1")
        service.reportPost(post.id)
        val reported = repository.findById(post.id)!!
        assertEquals(1, reported.reportCount)
        service.moderatePost(post.id, true, true)
        val moderated = repository.findById(post.id)!!
        assertTrue(moderated.deleted)
        assertEquals(0, moderated.reportCount)
    }

    @Test
    fun `suspended user cannot create post`() = runBlocking {
        val user = User(id = "u5", name = "n", gender = "M", birthYear = 1990)
        userRepo.save(user)
        val until = java.time.Instant.now().plusSeconds(60)
        userRepo.save(user.copy(suspendedUntil = until))
        try {
            service.createPost("no", null, null, user.id, "a5")
            assertTrue(false)
        } catch (e: IllegalStateException) {
            assertTrue(true)
        }
    }
}
