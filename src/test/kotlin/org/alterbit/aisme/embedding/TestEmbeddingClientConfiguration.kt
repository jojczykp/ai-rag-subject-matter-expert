package org.alterbit.aisme.embedding

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("fake-embedding-client")
class TestEmbeddingClientConfiguration {
    @Bean
    fun testEmbeddingClient(): EmbeddingClient =
        object : EmbeddingClient {
            override fun embed(text: String): EmbeddingVector {
                require(text.isNotBlank()) { "text must not be blank" }

                return EmbeddingVector(
                    values = List(TEST_EMBEDDING_DIMENSIONS) { index -> if (index == 0) 1.0 else 0.0 },
                    model = EmbeddingModelProperties().metadata,
                )
            }
        }

    private companion object {
        const val TEST_EMBEDDING_DIMENSIONS = 384
    }
}
