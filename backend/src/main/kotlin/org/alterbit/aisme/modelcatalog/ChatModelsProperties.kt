package org.alterbit.aisme.modelcatalog

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Name

@ConfigurationProperties(prefix = "aisme.chat")
data class ChatModelsProperties(
    @param:Name("default-model-id")
    val defaultModelId: String? = null,
    @param:Name("runtimes")
    val chatRuntimesById: Map<String, ChatModelRuntimeConfigProperties>,
    @param:Name("models")
    val chatModelsById: Map<String, ChatModelProperties>,
) {
    init {
        require(defaultModelId == null || defaultModelId.isNotBlank()) {
            "aisme.chat.default-model-id must not be blank when configured"
        }
    }
}
