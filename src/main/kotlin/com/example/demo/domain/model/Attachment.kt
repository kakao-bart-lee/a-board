package com.example.demo.domain.model

enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO
}

data class Attachment(
    val type: AttachmentType,
    val url: String
)
