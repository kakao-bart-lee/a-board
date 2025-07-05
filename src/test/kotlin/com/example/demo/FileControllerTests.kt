package com.example.demo

import com.example.demo.adapter.inmemory.InMemoryFileStorageAdapter
import com.example.demo.adapter.web.FileController
import com.example.demo.adapter.web.PresignedUrlRequest
import com.example.demo.application.FileService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FileControllerTests {

    private val fileStorageAdapter = InMemoryFileStorageAdapter()
    private val fileService = FileService(fileStorageAdapter)
    private val fileController = FileController(fileService)

    @Test
    fun `should return a presigned url`() = runBlocking {
        // Arrange
        val request = PresignedUrlRequest("test.jpg", "image/jpeg")

        // Act
        val response = fileController.getPresignedUrl(request)

        // Assert
        assertTrue(response.url.contains("/files/"))
        assertTrue(response.url.endsWith("-test.jpg"))
        assertTrue(response.fileKey.contains("uploads/"))
        assertTrue(response.fileKey.endsWith("-test.jpg"))
    }
}
