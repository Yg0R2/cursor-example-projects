package com.example.user.persistence

import com.example.core.persistence.ExampleRepository
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID>, ExampleRepository {

    fun findByUsername(username: String): UserEntity?

}
