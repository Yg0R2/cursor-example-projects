package com.example.user.web

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
    fun me(@AuthenticationPrincipal principal: User?): ResponseEntity<UserResponse> =
        principal
            ?.let { userRepository.findByUsername(it.username) }
            ?.let {
                UserResponse(
                    id = it.id,
                    username = it.username,
                    roles = it.roles,
                )
            }?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

}
