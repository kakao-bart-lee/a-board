package com.example.demo.domain.model

import java.util.UUID
import com.fasterxml.jackson.annotation.JsonIgnore

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val postId: String,
    @field:JsonIgnore val authorId: String,
    val anonymousId: String,
    val text: String,
    val parentCommentId: String? = null,
    val replies: MutableList<Comment> = mutableListOf(),
    var byPostAuthor: Boolean = false,
    var deleted: Boolean = false,
)

data class Post(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val imageUrl: String? = null,
    val gender: String? = null,
    @field:JsonIgnore val authorId: String,
    val anonymousId: String,
    val comments: MutableList<Comment> = mutableListOf(),
    var viewCount: Int = 0,
    var deleted: Boolean = false,
    var reportCount: Int = 0
)
