package com.example.service.application

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ServiceTemplateApplicationIntegrationTest {

    @Test
    fun contextLoads() {
        // Verifies the Spring context starts with all beans (e.g. DummyRequest, DummyServiceClient, DummyRepository, DummyRestController, DummyService).
    }

}
