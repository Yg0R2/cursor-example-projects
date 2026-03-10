package com.example.core.service

import com.example.core.api.model.ExampleResponse
import com.example.core.persistence.model.ExampleEntity

interface ExampleMapper<E : ExampleEntity, R : ExampleResponse> {

    fun map(entity: E): R

}
