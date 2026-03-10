package com.example.service.service

import com.example.core.api.exception.NotFoundException
import com.example.core.service.ExampleService
import com.example.service.api.model.DummyResponse
import com.example.service.persistence.DummyRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DummyService(
    private val dummyRepository: DummyRepository,
    private val dummyMapper: DummyMapper
) : ExampleService {

    suspend fun getById(id: UUID): DummyResponse =
        dummyRepository.findById(id)
            .map(dummyMapper::map)
            .orElseThrow { NotFoundException("DummyEntity with id $id not found") }

}
