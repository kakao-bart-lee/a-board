package com.example.demo.application

import com.example.demo.domain.port.FileStoragePort
import com.example.demo.domain.port.PresignedUrlResponse
import org.springframework.stereotype.Service

@Service
class FileService(private val fileStoragePort: FileStoragePort) {

    suspend fun generatePresignedUrl(fileName: String, contentType: String): PresignedUrlResponse {
        // You can add validation logic here (e.g., file size, type, user permissions)
        return fileStoragePort.generatePresignedUrl(fileName, contentType)
    }
}
