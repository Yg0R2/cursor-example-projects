package com.example.auth.application

import com.example.user.persistence.UserEntity
import com.example.user.persistence.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(LoginLogoutIntegrationTest.TestUserConfig::class)
class LoginLogoutIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun unauthenticatedAccessRedirectsToLogin() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/me"))
            .andExpect(MockMvcResultMatchers.status().isFound)
            .andExpect(MockMvcResultMatchers.redirectedUrlPattern("**/login"))
    }

    @Test
    fun loginWithValidCredentialsSucceeds() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/login")
                .param("username", "test")
                .param("password", "test")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andExpect(MockMvcResultMatchers.status().isFound)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/"))
    }

    @Test
    fun loginWithInvalidCredentialsFails() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/login")
                .param("username", "test")
                .param("password", "wrongpassword")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andExpect(MockMvcResultMatchers.status().isFound)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/login?error"))
    }

    @Test
    fun logoutSucceeds() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/logout")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("test").roles("ADMIN"))
        )
            .andExpect(MockMvcResultMatchers.status().isFound)
            .andExpect(MockMvcResultMatchers.redirectedUrl("/login?logout"))
    }

    @Test
    fun authenticatedUserCanAccessProtectedEndpoint() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .with(SecurityMockMvcRequestPostProcessors.user("test").roles("ADMIN"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.username").value("test"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.roles").isArray)
    }

    @TestConfiguration
    class TestUserConfig(
        private val userRepository: UserRepository,
        private val passwordEncoder: PasswordEncoder
    ) {
        @Bean
        fun testUserInitializer(): TestUserInitializer =
            TestUserInitializer(userRepository, passwordEncoder)
    }

    class TestUserInitializer(
        private val userRepository: UserRepository,
        private val passwordEncoder: PasswordEncoder
    ) {
        @jakarta.annotation.PostConstruct
        fun createTestUser() {
            if (userRepository.findByUsername("test") == null) {
                userRepository.save(
                    UserEntity(
                        username = "test",
                        password = passwordEncoder.encode("test")!!,
                        roles = mutableSetOf("ROLE_ADMIN")
                    )
                )
            }
        }
    }
}
