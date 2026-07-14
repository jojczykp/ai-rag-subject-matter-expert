package org.alterbit.aisme.modelcatalog

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Name

@ConfigurationProperties(prefix = "aisme")
data class ConfiguredChatModelsProperties(
    val runtimes: Map<String, ChatRuntimeProperties>,
    @param:Name("chat-models")
    val chatModelsById: Map<String, ChatModelProperties>,
)
