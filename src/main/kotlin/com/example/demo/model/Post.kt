package com.example.demo.model

import java.util.UUID

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val postId: String,
    val text: String
)

data class Post(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val imageUrl: String? = null,
    val gender: String? = null,
    val comments: MutableList<Comment> = mutableListOf()
)
