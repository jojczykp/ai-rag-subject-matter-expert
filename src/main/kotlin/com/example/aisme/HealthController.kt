package com.example.aisme

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/")
    fun index(): GreetingResponse = GreetingResponse(
        message = "AI Subject Matter Expert service is running",
    )
}

data class GreetingResponse(
    val message: String,
)
