package com.example.service.web

import com.example.core.web.ExampleRestController
import com.example.service.service.DummyService
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DummyRestController(
    private val dummyService: DummyService
) : ExampleRestController {

    suspend fun getById(id: UUID) =
        dummyService.getById(id)

}
