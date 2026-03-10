package com.example.core.api.exception

class NotFoundException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException()
