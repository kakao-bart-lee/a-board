package com.example.demo.adapter.inmemory

import com.example.demo.domain.port.FileStoragePort
import com.example.demo.domain.port.PresignedUrlResponse
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InMemoryFileStorageAdapter : FileStoragePort {

    private val storage = mutableMapOf<String, ByteArray>()

    override suspend fun generatePresignedUrl(fileName: String, contentType: String): PresignedUrlResponse {
        val fileKey = "uploads/${UUID.randomUUID()}-${fileName}"
        val presignedUrl = "http://localhost:8080/files/$fileKey" // Dummy URL for local testing
        return PresignedUrlResponse(url = presignedUrl, fileKey = fileKey)
    }
}
