package com.example.demo.adapter.web

import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.application.PostService
import com.example.demo.config.JwtAuthenticationToken
import kotlinx.coroutines.flow.Flow
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody req: CreatePostRequest, @AuthenticationPrincipal auth: JwtAuthenticationToken): Post =
        service.createPost(req.text, req.imageUrl, req.gender, auth.userId, auth.anonId)

    @GetMapping
    suspend fun list(): Flow<Post> = service.getPosts()

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): Post? = service.getPost(id)

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
