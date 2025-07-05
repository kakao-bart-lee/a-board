package com.example.demo.domain.port

import com.example.demo.domain.model.Attachment
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux

data class PresignedUrlResponse(
    val url: String,
    val fileKey: String
)

interface FileStoragePort {
    suspend fun generatePresignedUrl(fileName: String, contentType: String): PresignedUrlResponse
}
