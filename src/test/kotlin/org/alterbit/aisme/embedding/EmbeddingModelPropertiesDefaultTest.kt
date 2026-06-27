package org.alterbit.aisme.embedding

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("no-db")
@SpringBootTest
class EmbeddingModelPropertiesDefaultTest(
    private val properties: EmbeddingModelProperties,
) {
    @Test
    fun `uses default embedding model configuration`() {
        properties.id shouldBe "local-bge-small"
        properties.version shouldBe "1.5"
        properties.runtime shouldBe EmbeddingModelRuntime.ONNX
        properties.modelPath shouldBe "./models/bge-small-en-v1.5/model.onnx"
        properties.tokenizerPath shouldBe "./models/bge-small-en-v1.5/tokenizer.json"
        properties.dimensions shouldBe 384
    }
}
