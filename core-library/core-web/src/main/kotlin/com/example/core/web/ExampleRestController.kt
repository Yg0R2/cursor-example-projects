package com.example.core.web

import com.example.core.api.ExampleRequest
import com.example.core.api.ExampleResponse

/**
 * Example REST controller interface.
 * Framework-agnostic - no Spring or other framework dependencies.
 * Implementations will add framework-specific annotations.
 */
interface ExampleRestController {
    /**
     * Handles GET request to retrieve an example by ID.
     */
    fun getExample(id: String): ExampleResponse

    /**
     * Handles POST request to create/process an example.
     */
    fun postExample(request: ExampleRequest): ExampleResponse
}
