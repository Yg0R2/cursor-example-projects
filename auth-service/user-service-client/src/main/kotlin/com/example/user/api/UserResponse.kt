package com.example.user.api

data class UserResponse(
    val id: Long,
    val username: String,
    val roles: Set<String>
)
