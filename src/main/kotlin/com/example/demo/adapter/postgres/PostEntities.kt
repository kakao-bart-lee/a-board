package com.example.demo.adapter.postgres

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("posts")
data class PostEntity(
    @Id var id: String?,
    var text: String,
    var imageUrl: String?,
    var gender: String?,
    var authorId: String,
    var anonymousId: String,
    var viewCount: Int,
    var deleted: Boolean,
    var reportCount: Int
)

@Table("comments")
data class CommentEntity(
    @Id var id: String?,
    var postId: String,
    var authorId: String,
    var anonymousId: String,
    var text: String,
    var parentCommentId: String?,
    var byPostAuthor: Boolean,
    var deleted: Boolean
)

@Table("users")
data class UserEntity(
    @Id var id: String?,
    var name: String,
    var email: String,
    var password: String,
    var gender: String,
    var birthYear: Int,
    var profileImageUrls: String,
    var location: String?,
    var preferredLanguage: String?,
    var aboutMe: String?,
    var role: String,
    var suspendedUntil: java.time.Instant?,
    var verified: Boolean,
    var verificationCode: String?
)
