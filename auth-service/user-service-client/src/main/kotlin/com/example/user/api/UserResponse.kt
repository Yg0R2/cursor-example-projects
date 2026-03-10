package com.example.user.api

import com.example.core.api.model.ExampleResponse
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val username: String,
    val roles: Set<String>
) : ExampleResponse
