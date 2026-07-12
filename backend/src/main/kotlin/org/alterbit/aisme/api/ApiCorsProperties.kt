package org.alterbit.aisme.api

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.api.cors")
data class ApiCorsProperties(
    val allowedOrigins: List<String> = listOf("http://localhost:5173"),
)
