package com.example.core.service

import com.example.core.api.ExampleRequest
import com.example.core.api.ExampleResponse
import com.example.core.persistence.ExampleEntity

/**
 * Example service interface for business logic.
 * Framework-agnostic - no Spring or other framework dependencies.
 */
interface ExampleService {
    /**
     * Processes a request and returns a response.
     */
    fun process(request: ExampleRequest): ExampleResponse

    /**
     * Finds an entity by its ID.
     */
    fun findById(id: String): ExampleEntity?

    /**
     * Saves an entity and returns the saved instance.
     */
    fun save(entity: ExampleEntity): ExampleEntity
}
