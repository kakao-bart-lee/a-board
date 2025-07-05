package com.example.demo.adapter.web

import com.example.demo.domain.model.Attachment
import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.application.PostService
import com.example.demo.config.JwtAuthenticationToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/posts")
/**
 * REST endpoints for working with posts and comments. All routes require
 * authentication except for creating a user and obtaining a token.
 */
class PostController(private val service: PostService) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class CreatePostRequest(
        val text: String,
        val attachments: List<Attachment> = emptyList(),
        val gender: String? = null,
    )

    data class CommentRequest(
        val text: String,
        val attachments: List<Attachment> = emptyList(),
        val parentCommentId: String? = null
    )

    data class UpdatePostRequest(
        val text: String? = null,
        val attachments: List<Attachment>? = null,
        val gender: String? = null,
    )

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody req: CreatePostRequest, @AuthenticationPrincipal auth: JwtAuthenticationToken): Post {
        log.info("Creating post for user: ${auth.userId}, anonId: ${auth.anonId}")
        return service.createPost(req.text, req.attachments, req.gender, auth.userId, auth.anonId)
    }

    @GetMapping
    suspend fun list(
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false) limit: Int?,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Flow<Post> {
        log.info("Listing posts with offset: $offset, limit: $limit for user: ${auth.userId}")
        return service.getPosts(offset, limit, auth.userId)
    }

    @GetMapping("/user/{userId}")
    suspend fun byUser(
        @PathVariable userId: String,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Flow<Post> {
        log.info("Getting posts by user: $userId, requester: ${auth.userId}")
        return service.getPostsByUser(userId, auth.userId)
    }

    @GetMapping("/reported")
    suspend fun reported(@AuthenticationPrincipal auth: JwtAuthenticationToken): Flow<Post> {
        log.info("Getting reported posts for user: ${auth.userId}")
        return if (auth.authorities.any { it.authority == "ADMIN" || it.authority == "MODERATOR" })
            service.getReportedPosts(auth.userId)
        else
            emptyFlow()
    }

    @GetMapping("/{id}")
    suspend fun get(
        @PathVariable id: String,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Post? {
        log.info("Getting post with id: $id for user: ${auth.userId}")
        return service.getPost(id, auth.userId)
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: String,
        @RequestBody req: UpdatePostRequest,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Post? {
        log.info("Updating post with id: $id for user: ${auth.userId}")
        return service.updatePost(
            id,
            req.text,
            req.attachments,
            req.gender,
            auth.userId,
            auth.authorities.any { it.authority == "ADMIN" }
        )
    }

    @PostMapping("/{id}/report")
    suspend fun report(@PathVariable id: String, @AuthenticationPrincipal auth: JwtAuthenticationToken): Post? {
        log.info("Reporting post with id: $id")
        return service.reportPost(id)
    }

    @PostMapping("/{id}/moderate")
    suspend fun moderate(
        @PathVariable id: String,
        @RequestParam(defaultValue = "false") delete: Boolean,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Post? {
        log.info("Moderating post with id: $id, delete: $delete by user: ${auth.userId}")
        return service.moderatePost(id, delete, auth.authorities.any { it.authority == "ADMIN" || it.authority == "MODERATOR" })
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun comment(@PathVariable id: String, @RequestBody req: CommentRequest, @AuthenticationPrincipal auth: JwtAuthenticationToken): Comment? {
        log.info("Adding comment to post with id: $id for user: ${auth.userId}, anonId: ${auth.anonId}")
        return service.addComment(id, req.text, req.attachments, auth.userId, auth.anonId, req.parentCommentId)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String, @AuthenticationPrincipal auth: JwtAuthenticationToken) {
        log.info("Deleting post with id: $id by user: ${auth.userId}")
        service.deletePost(id, auth.userId, auth.authorities.any { it.authority == "ADMIN" })
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    suspend fun deleteComment(
        @PathVariable postId: String,
        @PathVariable commentId: String,
        @RequestParam(required = false) parentCommentId: String?,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ) {
        log.info("Deleting comment with id: $commentId from post: $postId by user: ${auth.userId}")
        service.deleteComment(postId, commentId, auth.userId, auth.authorities.any { it.authority == "ADMIN" }, parentCommentId)
    }
}
