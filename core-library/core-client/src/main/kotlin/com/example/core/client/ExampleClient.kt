package com.example.core.client

import com.example.core.api.ExampleRequest
import com.example.core.api.ExampleResponse

/**
 * Example client interface for external service communication.
 * Framework-agnostic - no Spring or other framework dependencies.
 */
interface ExampleClient {
    /**
     * Sends a request and returns a response.
     */
    fun send(request: ExampleRequest): ExampleResponse

    /**
     * Sends a request asynchronously.
     */
    suspend fun sendAsync(request: ExampleRequest): ExampleResponse
}
