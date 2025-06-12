package com.example.demo.integration

import com.example.demo.adapter.inmemory.InMemoryPostRepository
import com.example.demo.application.PostService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PostModerationIntegrationTests {
    private val repo = InMemoryPostRepository()
    private val service = PostService(repo)

    @Test
    fun reportAndModerate() = runBlocking {
        val post = service.createPost("hello", null, null, "u1", "a1")
        service.reportPost(post.id)
        val reported = repo.findById(post.id)!!
        assertEquals(1, reported.reportCount)
        service.moderatePost(post.id, true, true)
        val moderated = repo.findById(post.id)!!
        assertTrue(moderated.deleted)
        assertEquals(0, moderated.reportCount)
    }
}
