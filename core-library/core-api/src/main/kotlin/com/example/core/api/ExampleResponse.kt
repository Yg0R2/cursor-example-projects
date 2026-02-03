package com.example.core.api

/**
 * Example response interface for the core API.
 * Framework-agnostic - no Spring or other framework dependencies.
 */
interface ExampleResponse {
    val id: String
    val result: String
    val success: Boolean
}
