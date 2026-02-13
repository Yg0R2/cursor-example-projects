package com.example.service.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.service"])
class ServiceTemplateApplication

fun main(args: Array<String>) {
    runApplication<ServiceTemplateApplication>(*args)
}
