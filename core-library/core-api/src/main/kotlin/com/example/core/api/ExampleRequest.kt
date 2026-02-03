package com.example.core.api

/**
 * Example request interface for the core API.
 * Framework-agnostic - no Spring or other framework dependencies.
 */
interface ExampleRequest {
    val id: String
    val data: String
}
