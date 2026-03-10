package com.example.auth.web

import com.example.user.api.UserResponse
import com.example.user.persistence.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository
) {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: User?): ResponseEntity<UserResponse> {
        if (principal == null) {
            return ResponseEntity.notFound().build()
        }
        val entity = userRepository.findByUsername(principal.username)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            UserResponse(
                id = entity.id,
                username = entity.username,
                roles = entity.roles
            )
        )
    }
}
