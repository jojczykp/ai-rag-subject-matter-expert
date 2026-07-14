package org.alterbit.aisme.modelcatalog

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Name

@ConfigurationProperties(prefix = "aisme")
data class ChatModelsProperties(
    @param:Name("chat-runtimes")
    val chatRuntimesById: Map<String, ChatModelRuntimeConfigProperties>,
    @param:Name("chat-models")
    val chatModelsById: Map<String, ChatModelProperties>,
)
