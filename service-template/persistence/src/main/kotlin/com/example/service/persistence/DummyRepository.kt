package com.example.service.persistence

import com.example.core.persistence.ExampleRepository
import com.example.service.persistence.model.DummyEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Dummy repository for service-template persistence module.
 * Extends or uses types from core-library spring-core-persistence.
 */
@Repository
interface DummyRepository : CrudRepository<DummyEntity, UUID>, ExampleRepository
