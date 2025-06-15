package com.example.demo.adapter.web

import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.application.PostService
import com.example.demo.config.JwtAuthenticationToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/posts")
class PostController(private val service: PostService) {
    data class CreatePostRequest(
        val text: String,
        val imageUrl: String? = null,
        val gender: String? = null,
    )

    data class CommentRequest(
        val text: String,
        val parentCommentId: String? = null
    )

    data class UpdatePostRequest(
        val text: String? = null,
        val imageUrl: String? = null,
        val gender: String? = null,
    )

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody req: CreatePostRequest, @AuthenticationPrincipal auth: JwtAuthenticationToken): Post =
        service.createPost(req.text, req.imageUrl, req.gender, auth.userId, auth.anonId)

    @GetMapping
    suspend fun list(
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false) limit: Int?,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Flow<Post> = service.getPosts(offset, limit, auth.userId)

    @GetMapping("/user/{userId}")
    suspend fun byUser(
        @PathVariable userId: String,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Flow<Post> = service.getPostsByUser(userId, auth.userId)

    @GetMapping("/reported")
    suspend fun reported(@AuthenticationPrincipal auth: JwtAuthenticationToken): Flow<Post> =
        if (auth.authorities.any { it.authority == "ADMIN" || it.authority == "MODERATOR" })
            service.getReportedPosts(auth.userId)
        else
            kotlinx.coroutines.flow.emptyFlow()

    @GetMapping("/{id}")
    suspend fun get(
        @PathVariable id: String,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Post? = service.getPost(id, auth.userId)

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: String,
        @RequestBody req: UpdatePostRequest,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Post? = service.updatePost(
        id,
        req.text,
        req.imageUrl,
        req.gender,
        auth.userId,
        auth.authorities.any { it.authority == "ADMIN" }
    )

    @PostMapping("/{id}/report")
    suspend fun report(@PathVariable id: String, @AuthenticationPrincipal auth: JwtAuthenticationToken): Post? =
        service.reportPost(id)

    @PostMapping("/{id}/moderate")
    suspend fun moderate(
        @PathVariable id: String,
        @RequestParam(defaultValue = "false") delete: Boolean,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ): Post? = service.moderatePost(id, delete, auth.authorities.any { it.authority == "ADMIN" || it.authority == "MODERATOR" })

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun comment(@PathVariable id: String, @RequestBody req: CommentRequest, @AuthenticationPrincipal auth: JwtAuthenticationToken): Comment? =
        service.addComment(id, req.text, auth.userId, auth.anonId, req.parentCommentId)

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String, @AuthenticationPrincipal auth: JwtAuthenticationToken) {
        service.deletePost(id, auth.userId, auth.authorities.any { it.authority == "ADMIN" })
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    suspend fun deleteComment(
        @PathVariable postId: String,
        @PathVariable commentId: String,
        @RequestParam(required = false) parentCommentId: String?,
        @AuthenticationPrincipal auth: JwtAuthenticationToken
    ) {
        service.deleteComment(postId, commentId, auth.userId, auth.authorities.any { it.authority == "ADMIN" }, parentCommentId)
    }
}
