package com.example.demo.domain.model

import java.util.UUID
import java.time.Instant
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * In-memory representation of a single comment on a post. `authorId` is
 * ignored during JSON serialization so clients only see the anonymous id.
 */
data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val postId: String,
    @field:JsonIgnore val authorId: String,
    val anonymousId: String,
    val createdAt: Instant = Instant.now(),
    val text: String,
    val parentCommentId: String? = null,
    val replies: MutableList<Comment> = mutableListOf(),
    var byPostAuthor: Boolean = false,
    var deleted: Boolean = false,
    var canDelete: Boolean = false,
)
/**
 * Post created by a user. Comments and view counts are embedded for convenience.
 * The authorId is kept server-side only.
 */

data class Post(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val imageUrl: String? = null,
    val gender: String? = null,
    @field:JsonIgnore val authorId: String,
    val anonymousId: String,
    val createdAt: Instant = Instant.now(),
    val comments: MutableList<Comment> = mutableListOf(),
    var viewCount: Int = 0,
    var deleted: Boolean = false,
    var reportCount: Int = 0,
    var canDelete: Boolean = false,
)
