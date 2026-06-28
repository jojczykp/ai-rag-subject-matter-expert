package org.alterbit.aisme.embedding

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import jakarta.annotation.PreDestroy
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OnnxEmbeddingClient private constructor(
    private val properties: EmbeddingModelProperties,
    private val model: LoadedEmbeddingModel,
) : EmbeddingClient {
    @Autowired
    constructor(
        properties: EmbeddingModelProperties,
    ) : this(
        properties = properties,
        model = DefaultOnnxEmbeddingModelLoader(properties).load(),
    )

    internal constructor(
        properties: EmbeddingModelProperties,
        loader: OnnxEmbeddingModelLoader,
    ) : this(
        properties = properties,
        model = loader.load(),
    )

    override fun embed(text: String): EmbeddingVector {
        require(text.isNotBlank()) { "text must not be blank" }

        val values = model.embed(text)
        require(values.size == properties.metadata.dimensions) {
            "ONNX embedding dimensions ${values.size} did not match configured dimensions ${properties.metadata.dimensions}"
        }

        return EmbeddingVector(
            values = values,
            model = properties.metadata,
        )
    }

    @PreDestroy
    fun close() {
        model.close()
    }
}

internal interface LoadedEmbeddingModel {
    fun embed(text: String): List<Double>

    fun close()
}

internal fun interface OnnxEmbeddingModelLoader {
    fun load(): LoadedEmbeddingModel
}

private class DefaultOnnxEmbeddingModelLoader(
    private val properties: EmbeddingModelProperties,
) : OnnxEmbeddingModelLoader {
    override fun load(): LoadedEmbeddingModel {
        require(properties.runtime == EmbeddingModelRuntime.ONNX) {
            "Unsupported embedding model runtime: ${properties.runtime}"
        }

        val modelPath = requireReadableFile(properties.modelPath, "model")
        val tokenizerPath = requireReadableFile(properties.tokenizerPath, "tokenizer")

        return try {
            val environment = OrtEnvironment.getEnvironment()
            val session = environment.createSession(modelPath.toString())
            val tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath)

            OnnxEmbeddingModel(
                environment = environment,
                session = session,
                tokenizer = tokenizer,
            )
        } catch (ex: IOException) {
            throw EmbeddingException("Failed to load ONNX embedding tokenizer: $tokenizerPath", ex)
        } catch (ex: OrtException) {
            throw EmbeddingException("Failed to load ONNX embedding model: $modelPath", ex)
        }
    }

    private fun requireReadableFile(
        configuredPath: String,
        label: String,
    ): Path {
        val path = Path(configuredPath)
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw EmbeddingException("Configured ONNX embedding $label file is not readable: $path")
        }
        return path
    }
}

private class OnnxEmbeddingModel(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val tokenizer: HuggingFaceTokenizer,
) : LoadedEmbeddingModel {
    override fun embed(text: String): List<Double> {
        val encoding = tokenizer.encode(text)
        val tokenIds = encoding.getIds()
        val tensors = session.inputNames.associateWith { inputName ->
            when (inputName) {
                "input_ids" -> OnnxTensor.createTensor(environment, arrayOf(tokenIds))
                "attention_mask" -> OnnxTensor.createTensor(environment, arrayOf(encoding.getAttentionMask()))
                "token_type_ids" -> OnnxTensor.createTensor(environment, arrayOf(encoding.getTypeIds()))
                else -> throw EmbeddingException("Unsupported ONNX embedding model input: $inputName")
            }
        }

        try {
            session.run(tensors).use { result ->
                return normalize(extractVector(result.get(0)))
            }
        } finally {
            tensors.values.forEach(OnnxTensor::close)
        }
    }

    override fun close() {
        tokenizer.close()
        session.close()
    }

    private fun extractVector(output: OnnxValue): List<Double> =
        when (val value = output.value) {
            is Array<*> -> extractVector(value)
            is FloatArray -> value.map(Float::toDouble)
            else -> throw EmbeddingException("Unsupported ONNX embedding output type: ${value::class.java.name}")
        }

    private fun extractVector(value: Array<*>): List<Double> {
        val first = value.firstOrNull()
            ?: throw EmbeddingException("ONNX embedding output was empty")

        return when (first) {
            is FloatArray -> first.map(Float::toDouble)
            is Array<*> -> extractVector(first)
            else -> throw EmbeddingException("Unsupported ONNX embedding output element type: ${first::class.java.name}")
        }
    }

    private fun normalize(values: List<Double>): List<Double> {
        val magnitude = kotlin.math.sqrt(values.sumOf { it * it })
        if (magnitude == 0.0) {
            throw EmbeddingException("ONNX embedding output vector had zero magnitude")
        }

        return values.map { it / magnitude }
    }
}
