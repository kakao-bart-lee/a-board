package com.example.demo

import com.example.demo.adapter.inmemory.InMemoryPostRepository
import com.example.demo.adapter.inmemory.InMemoryUserRepository
import com.example.demo.application.PostService
import com.example.demo.application.UserService
import com.example.demo.config.JwtService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AdditionalServiceTests {
    private val postRepo = InMemoryPostRepository()
    private val userRepo = InMemoryUserRepository()
    private val postService = PostService(postRepo, userRepo)
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

    @Test
    fun `deleting user keeps posts`() = runBlocking {
        val user = userService.createUser("n", "F", 2000, emptyList(), null, null, null)
        val post = postService.createPost("hello", null, null, user.id, "a1")
        val deleted = userService.deleteUser(user.id)
        assertTrue(deleted)
        val remaining = postRepo.findById(post.id)
        assertNotNull(remaining)
    }

    @Test
    fun `suspended user cannot comment`() = runBlocking {
        val user = userService.createUser("n", "M", 1990, emptyList(), null, null, null)
        val until = java.time.Instant.now().plusSeconds(120)
        userService.suspendUser(user.id, until)
        val post = postService.createPost("hello", null, null, "other", "a2")
        try {
            postService.addComment(post.id, "hi", user.id, "anon")
            assertTrue(false)
        } catch (e: IllegalStateException) {
            assertTrue(true)
        }
    }

    @Test
    fun `posts indicate deletable by author`() = runBlocking {
        val p1 = postService.createPost("one", null, null, "u1", "a1")
        val p2 = postService.createPost("two", null, null, "u2", "a2")

        val posts = postService.getPosts(requesterId = "u1").toList()
        val mine = posts.find { it.id == p1.id }!!
        val other = posts.find { it.id == p2.id }!!
        assertTrue(mine.canDelete)
        assertFalse(other.canDelete)
    }

    @Test
    fun `comments indicate deletable by author`() = runBlocking {
        val post = postService.createPost("cpost", null, null, "u1", "a1")
        val c1 = postService.addComment(post.id, "mine", "u1", "a1")!!
        val c2 = postService.addComment(post.id, "theirs", "u2", "a2")!!

        val fetched = postService.getPost(post.id, requesterId = "u1")!!
        val mc1 = fetched.comments.find { it.id == c1.id }!!
        val mc2 = fetched.comments.find { it.id == c2.id }!!
        assertTrue(mc1.canDelete)
        assertFalse(mc2.canDelete)
    }
}
