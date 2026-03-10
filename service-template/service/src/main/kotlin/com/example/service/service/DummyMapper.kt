package com.example.service.service

import com.example.core.service.ExampleMapper
import com.example.service.api.model.DummyResponse
import com.example.service.persistence.model.DummyEntity
import org.springframework.stereotype.Component

@Component
class DummyMapper : ExampleMapper<DummyEntity, DummyResponse> {

    override fun map(entity: DummyEntity): DummyResponse =
        DummyResponse(
            id = entity.id.toString(),
        )

}
