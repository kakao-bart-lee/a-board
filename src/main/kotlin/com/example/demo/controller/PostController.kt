package com.example.demo.controller

import com.example.demo.model.Comment
import com.example.demo.model.Post
import com.example.demo.service.PostService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/posts")
class PostController(private val service: PostService) {
    data class CreatePostRequest(val text: String, val imageUrl: String? = null, val gender: String? = null)
    data class CommentRequest(val text: String)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: CreatePostRequest): Post =
        service.createPost(req.text, req.imageUrl, req.gender)

    @GetMapping
    fun list(): List<Post> = service.getPosts()

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): Post? = service.getPost(id)

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    fun comment(@PathVariable id: String, @RequestBody req: CommentRequest): Comment? =
        service.addComment(id, req.text)
}
