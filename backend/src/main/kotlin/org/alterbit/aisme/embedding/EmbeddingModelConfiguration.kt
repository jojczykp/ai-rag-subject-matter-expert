package org.alterbit.aisme.embedding

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class EmbeddingModelConfiguration {
    @Bean
    fun embeddingModelProperties(properties: EmbeddingProperties): EmbeddingModelProperties =
        properties.activeModel()
}
