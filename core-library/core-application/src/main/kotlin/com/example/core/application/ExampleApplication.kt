package com.example.core.application

/**
 * Example application interface.
 * Framework-agnostic - no Spring or other framework dependencies.
 * Implementations will provide framework-specific application bootstrap.
 */
interface ExampleApplication {
    /**
     * Starts the application.
     */
    fun start(args: Array<String>)

    /**
     * Stops the application gracefully.
     */
    fun stop()
}
