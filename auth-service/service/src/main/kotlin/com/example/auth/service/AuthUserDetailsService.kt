package com.example.auth.service

import com.example.user.persistence.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AuthUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val entity = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")
        val authorities = entity.roles.map { SimpleGrantedAuthority(it) }.toSet()
        return User(entity.username, entity.password, authorities)
    }
}
