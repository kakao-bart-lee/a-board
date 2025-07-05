import com.example.demo.adapter.inmemory.InMemoryFileStorageAdapter
import com.example.demo.adapter.inmemory.InMemoryNotificationRepository
import com.example.demo.adapter.inmemory.InMemoryPostRepository
import com.example.demo.adapter.inmemory.InMemoryUserRepository
import com.example.demo.application.PostService
import com.example.demo.domain.model.User
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MultiUserWorkflowIntegrationTests {
    private val postRepo = InMemoryPostRepository()
    private val userRepo = InMemoryUserRepository()
    private val notificationRepo = InMemoryNotificationRepository()
    private val fileStorageAdapter = InMemoryFileStorageAdapter()
    private val service = PostService(postRepo, userRepo, notificationRepo, fileStorageAdapter)

    @Test
    fun `post created by one user is visible to another`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))

        val post = service.createPost("hello", emptyList(), null, "u1", "a1")

        // another user loads all posts
        val posts = service.getPosts().toList()
        assertTrue(posts.any { it.id == post.id })

        // retrieving the post increments view count
        val fetched = service.getPost(post.id)
        assertEquals(1, fetched?.viewCount)
    }

    @Test
    fun `only author can delete post`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))

        val post = service.createPost("bye", emptyList(), null, "u1", "a1")

        // deletion attempt by different user should fail
        val byOther = service.deletePost(post.id, "u2", false)
        assertFalse(byOther)
        val still = postRepo.findById(post.id)!!
        assertFalse(still.deleted)

        // author can delete
        val byAuthor = service.deletePost(post.id, "u1", false)
        assertTrue(byAuthor)
        val deleted = postRepo.findById(post.id)!!
        assertTrue(deleted.deleted)
    }

    @Test
    fun `comments can only be deleted by their authors`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))

        val post = service.createPost("hello", emptyList(), null, "u1", "a1")
        val comment = service.addComment(post.id, "hi", emptyList(), "u2", "a2")!!
        val reply = service.addComment(post.id, "reply", emptyList(), "u1", "a1", comment.id)!!

        // different user cannot delete the author's reply
        val failDelete = service.deleteComment(post.id, reply.id, "u2", false, comment.id)
        assertFalse(failDelete)
        assertFalse(reply.deleted)

        // author deletes their own reply
        val successDelete = service.deleteComment(post.id, reply.id, "u1", false, comment.id)
        assertTrue(successDelete)
        assertTrue(postRepo.findById(post.id)!!.comments.first().replies.first().deleted)

        // post author cannot delete someone else's comment
        val failDeleteComment = service.deleteComment(post.id, comment.id, "u1", false)
        assertFalse(failDeleteComment)
        assertFalse(postRepo.findById(post.id)!!.comments.first().deleted)

        // comment author can delete their comment
        val successDeleteComment = service.deleteComment(post.id, comment.id, "u2", false)
        assertTrue(successDeleteComment)
        assertTrue(postRepo.findById(post.id)!!.comments.first().deleted)
    }

    @Test
    fun `updating and moderating posts with multiple users`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))

        val post = service.createPost("start", emptyList(), null, "u1", "a1")

        // views from another user
        service.getPost(post.id)
        service.getPost(post.id)
        assertEquals(2, postRepo.findById(post.id)!!.viewCount)

        // non author cannot update
        val unauthorized = service.updatePost(post.id, "hack", emptyList(), null, "u2", false)
        assertNull(unauthorized)

        // author can update
        val updated = service.updatePost(post.id, "updated", emptyList(), null, "u1", false)
        assertEquals("updated", updated?.text)

        // report and moderate
        service.reportPost(post.id)
        assertEquals(1, postRepo.findById(post.id)!!.reportCount)
        val moderated = service.moderatePost(post.id, false, true)!!
        assertEquals(0, moderated.reportCount)

        // only author can delete post
        assertFalse(service.deletePost(post.id, "u2", false))
        assertTrue(service.deletePost(post.id, "u1", false))
        val deleted = postRepo.findById(post.id)!!
        val before = deleted.viewCount
        service.getPost(post.id)
        assertEquals(before, deleted.viewCount)
    }

    @Test
    fun `multiple posts are visible to all users`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))
        userRepo.save(User(id = "u3", name = "u3", email = "u3@test.com", password = "p", gender = "M", birthYear = 1992))

        val post1 = service.createPost("p1", emptyList(), null, "u1", "a1")
        val post2 = service.createPost("p2", emptyList(), null, "u2", "a2")

        val posts = service.getPosts().toList()
        assertEquals(2, posts.size)
        assertTrue(posts.any { it.id == post1.id })
        assertTrue(posts.any { it.id == post2.id })

        val fromUser3 = service.getPosts().toList()
        assertEquals(2, fromUser3.size)
    }

    @Test
    fun `retrieve posts by author`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))

        val p1 = service.createPost("first", emptyList(), null, "u1", "a1")
        val p2 = service.createPost("second", emptyList(), null, "u1", "a1")
        service.createPost("other", emptyList(), null, "u2", "a2")

        val user1Posts = service.getPostsByUser("u1").toList()
        assertEquals(setOf(p1.id, p2.id), user1Posts.map { it.id }.toSet())
        val user2Posts = service.getPostsByUser("u2").toList()
        assertEquals(1, user2Posts.size)
        assertEquals("other", user2Posts.first().text)
    }

    @Test
    fun `complex comment thread workflow`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))
        userRepo.save(User(id = "u3", name = "u3", email = "u3@test.com", password = "p", gender = "M", birthYear = 1992))

        val post = service.createPost("hi", emptyList(), null, "u1", "a1")
        val comment = service.addComment(post.id, "from u2", emptyList(), "u2", "a2")!!
        val reply1 = service.addComment(post.id, "reply by u1", emptyList(), "u1", "a1", comment.id)!!
        val reply2 = service.addComment(post.id, "reply by u3", emptyList(), "u3", "a3", comment.id)!!

        val loaded = service.getPost(post.id)!!
        val loadedComment = loaded.comments.first()
        assertEquals(2, loadedComment.replies.size)
        assertTrue(loadedComment.replies.any { it.id == reply1.id })
        assertTrue(loadedComment.replies.any { it.id == reply2.id })

        // unauthorized deletion attempts
        assertFalse(service.deleteComment(post.id, reply2.id, "u2", false, comment.id))
        assertFalse(service.deleteComment(post.id, comment.id, "u1", false))

        // proper deletions
        assertTrue(service.deleteComment(post.id, reply2.id, "u3", false, comment.id))
        assertTrue(service.deleteComment(post.id, comment.id, "u2", false))

        val after = postRepo.findById(post.id)!!
        val firstComment = after.comments.first()
        assertTrue(firstComment.deleted)
        assertTrue(firstComment.replies.first { it.id == reply2.id }.deleted)
    }

    @Test
    fun `nested comment thread visible to others`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))
        userRepo.save(User(id = "u3", name = "u3", email = "u3@test.com", password = "p", gender = "M", birthYear = 1992))

        val post = service.createPost("thread", emptyList(), null, "u1", "a1")
        val comment = service.addComment(post.id, "c1", emptyList(), "u2", "a2")!!
        val reply1 = service.addComment(post.id, "r1", emptyList(), "u1", "a1", comment.id)!!
        val reply2 = service.addComment(post.id, "r2", emptyList(), "u2", "a2", comment.id)!!

        // another user fetches the post and sees the comment thread
        val fetched = service.getPost(post.id)!!
        val top = fetched.comments.find { it.id == comment.id }
        assertNotNull(top)
        assertEquals(2, top!!.replies.size)
        assertTrue(top.replies.any { it.id == reply1.id })
        assertTrue(top.replies.any { it.id == reply2.id })
    }

    @Test
    fun `nested comment deletion rights`() = runBlocking {
        userRepo.save(User(id = "u1", name = "u1", email = "u1@test.com", password = "p", gender = "M", birthYear = 1990))
        userRepo.save(User(id = "u2", name = "u2", email = "u2@test.com", password = "p", gender = "F", birthYear = 1991))
        userRepo.save(User(id = "u3", name = "u3", email = "u3@test.com", password = "p", gender = "F", birthYear = 1992))

        val post = service.createPost("lifecycle", emptyList(), null, "u1", "a1")
        val comment = service.addComment(post.id, "c", emptyList(), "u2", "a2")!!
        val r1 = service.addComment(post.id, "r1", emptyList(), "u1", "a1", comment.id)!!
        val r2 = service.addComment(post.id, "r2", emptyList(), "u3", "a3", comment.id)!!

        // post author cannot delete other's reply
        assertFalse(service.deleteComment(post.id, r2.id, "u1", false, comment.id))
        assertFalse(postRepo.findById(post.id)!!.comments.first().replies.find { it.id == r2.id }!!.deleted)

        // comment author cannot delete post author's reply
        assertFalse(service.deleteComment(post.id, r1.id, "u2", false, comment.id))
        assertFalse(postRepo.findById(post.id)!!.comments.first().replies.find { it.id == r1.id }!!.deleted)

        // reply authors delete their own comments
        assertTrue(service.deleteComment(post.id, r1.id, "u1", false, comment.id))
        assertTrue(postRepo.findById(post.id)!!.comments.first().replies.find { it.id == r1.id }!!.deleted)
        assertTrue(service.deleteComment(post.id, r2.id, "u3", false, comment.id))
        assertTrue(postRepo.findById(post.id)!!.comments.first().replies.find { it.id == r2.id }!!.deleted)

        // comment author deletes original comment
        assertTrue(service.deleteComment(post.id, comment.id, "u2", false))
        assertTrue(postRepo.findById(post.id)!!.comments.first().deleted)

        val fetched = service.getPost(post.id)!!
        val fetchedComment = fetched.comments.find { it.id == comment.id }
        assertTrue(fetchedComment!!.deleted)
        assertEquals(2, fetchedComment.replies.count { it.deleted })
    }
}
