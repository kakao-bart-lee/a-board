package com.example.demo.adapter.web

import com.example.demo.domain.model.Comment
import com.example.demo.domain.model.Post
import com.example.demo.application.PostService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/posts")
class PostController(private val service: PostService) {
    data class CreatePostRequest(val text: String, val imageUrl: String? = null, val gender: String? = null)
    data class CommentRequest(val text: String)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@RequestBody req: CreatePostRequest): Post =
        service.createPost(req.text, req.imageUrl, req.gender)

    @GetMapping
    suspend fun list(): Flow<Post> = service.getPosts()

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): Post? = service.getPost(id)

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun comment(@PathVariable id: String, @RequestBody req: CommentRequest): Comment? =
        service.addComment(id, req.text)
}
