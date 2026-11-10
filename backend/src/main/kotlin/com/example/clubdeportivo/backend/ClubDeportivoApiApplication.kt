package com.example.clubdeportivo.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ClubDeportivoApiApplication

fun main(args: Array<String>) {
    runApplication<ClubDeportivoApiApplication>(*args)
}
