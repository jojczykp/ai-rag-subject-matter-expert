package org.alterbit.aisme.modelcatalog

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme")
data class ConfiguredChatModelsProperties(
    val runtimes: Map<String, ConfiguredChatRuntimeProperties>,
    val chatModels: List<ConfiguredChatModelProperties>,
)
