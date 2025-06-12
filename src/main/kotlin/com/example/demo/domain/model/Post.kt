package com.example.demo.domain.model

import java.util.UUID

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val postId: String,
    val authorId: String,
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
    val authorId: String,
    val comments: MutableList<Comment> = mutableListOf(),
    var viewCount: Int = 0,
    var deleted: Boolean = false
)
