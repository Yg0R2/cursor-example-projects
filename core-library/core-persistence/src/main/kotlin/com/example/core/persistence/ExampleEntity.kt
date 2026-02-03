package com.example.core.persistence

/**
 * Example entity interface for database persistence.
 * Framework-agnostic - no Spring or other framework dependencies.
 */
interface ExampleEntity {
    val id: String
    val name: String
    val createdAt: Long
    val updatedAt: Long
}
