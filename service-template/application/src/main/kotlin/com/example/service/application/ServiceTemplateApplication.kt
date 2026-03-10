package com.example.service.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.example.service"])
@EnableJpaRepositories(basePackages = ["com.example.service.persistence"])
@EntityScan(basePackages = ["com.example.service.persistence"])
class ServiceTemplateApplication

fun main(args: Array<String>) {
    runApplication<ServiceTemplateApplication>(*args)
}
