package org.alterbit.aisme.modelcatalog

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Name

@ConfigurationProperties(prefix = "aisme.chat")
data class ChatModelsProperties(
    @param:Name("runtimes")
    val chatRuntimesById: Map<String, ChatModelRuntimeConfigProperties>,
    @param:Name("models")
    val chatModelsById: Map<String, ChatModelProperties>,
)
