import com.example.demo.adapter.inmemory.InMemoryFileStorageAdapter
import com.example.demo.adapter.inmemory.InMemoryNotificationRepository
import com.example.demo.adapter.inmemory.InMemoryPostRepository
import com.example.demo.adapter.inmemory.InMemoryUserRepository
import com.example.demo.application.PostService
import com.example.demo.domain.model.User
import com.example.demo.domain.port.NotificationRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NotificationServiceTests {

    private lateinit var postService: PostService
    private lateinit var postRepository: InMemoryPostRepository
    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var fileStorageAdapter: InMemoryFileStorageAdapter

    private val userA = User(id = "user-a", name = "userA", email = "a@a.com", password = "password", gender = "male", birthYear = 2000)
    private val userB = User(id = "user-b", name = "userB", email = "b@b.com", password = "password", gender = "female", birthYear = 2001)
    private val userC = User(id = "user-c", name = "userC", email = "c@c.com", password = "password", gender = "male", birthYear = 2002)

    @BeforeEach
    fun setup() {
        postRepository = InMemoryPostRepository()
        userRepository = InMemoryUserRepository()
        notificationRepository = InMemoryNotificationRepository()
        fileStorageAdapter = InMemoryFileStorageAdapter()
        postService = PostService(postRepository, userRepository, notificationRepository, fileStorageAdapter)

        runBlocking {
            userRepository.save(userA)
            userRepository.save(userB)
            userRepository.save(userC)
        }
    }

    @Test
    fun `should send notification to post author when a comment is added`() = runBlocking {
        // Arrange
        val post = postService.createPost("Post by A", emptyList(), null, userA.id, "anon-a")

        // Act
        postService.addComment(post.id, "Comment by B", emptyList(), userB.id, "anon-b")

        // Assert
        val notifications = notificationRepository.findByUserId(userA.id)
        assertNotNull(notifications)
        assertEquals(1, notifications.size)
        val notification = notifications.first()
        assertEquals(userA.id, notification.userId)
        assertEquals(post.id, notification.sourcePostId)
        assertEquals("anon-b", notification.triggeringAnonymousId)
    }

    @Test
    fun `should send notification to comment author when a reply is added`() = runBlocking {
        // Arrange
        val post = postService.createPost("Post by A", emptyList(), null, userA.id, "anon-a")
        val comment = postService.addComment(post.id, "Comment by B", emptyList(), userB.id, "anon-b")!!

        // Act
        postService.addComment(post.id, "Reply by C", emptyList(), userC.id, "anon-c", comment.id)

        // Assert
        val notifications = notificationRepository.findByUserId(userB.id)
        assertEquals(1, notifications.size)
        val notification = notifications.first()
        assertEquals(userB.id, notification.userId)
        assertEquals(post.id, notification.sourcePostId)
        assertEquals(comment.id, notification.sourceCommentId)
        assertEquals("anon-c", notification.triggeringAnonymousId)
    }

    @Test
    fun `should not send notification to self`() = runBlocking {
        // Arrange
        val post = postService.createPost("Post by A", emptyList(), null, userA.id, "anon-a")

        // Act
        postService.addComment(post.id, "Comment by A", emptyList(), userA.id, "anon-a")

        // Assert
        val notifications = notificationRepository.findByUserId(userA.id)
        assertEquals(0, notifications.size)
    }

    @Test
    fun `should mark notification as read`() = runBlocking {
        // Arrange
        val post = postService.createPost("Post by A", emptyList(), null, userA.id, "anon-a")
        postService.addComment(post.id, "Comment by B", emptyList(), userB.id, "anon-b")
        val notification = notificationRepository.findByUserId(userA.id).first()

        // Act
        notificationRepository.markAsRead(notification.id)

        // Assert
        val updatedNotification = notificationRepository.findByUserId(userA.id).first()
        assertEquals(true, updatedNotification.read)
    }
}
