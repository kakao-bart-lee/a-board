package com.example.demo.adapter.web

import com.example.demo.application.FileService
import com.example.demo.domain.port.PresignedUrlResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class PresignedUrlRequest(
    val fileName: String,
    val contentType: String
)

@RestController
@RequestMapping("/api/files")
class FileController(private val fileService: FileService) {

    @PostMapping("/presigned-url")
    suspend fun getPresignedUrl(@RequestBody request: PresignedUrlRequest): PresignedUrlResponse {
        return fileService.generatePresignedUrl(request.fileName, request.contentType)
    }
}
