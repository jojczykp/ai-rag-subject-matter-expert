package org.alterbit.aisme.assets

import org.alterbit.aisme.chat.catalog.ChatModelRegistry
import org.alterbit.aisme.chat.catalog.ChatModelsProperties
import org.alterbit.aisme.embedding.catalog.EmbeddingModelRegistry
import org.alterbit.aisme.embedding.catalog.EmbeddingProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class ModelAssetApplicationContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val binder = Binder.get(applicationContext.environment)
        downloadMissingAssets(
            downloadableAssets(
                embeddingModelRegistry = binder.embeddingModelRegistry(),
                chatModelRegistry = binder.chatModelRegistry(),
            ),
        )
    }

    private fun downloadMissingAssets(assets: List<ModelAsset>) {
        if (assets.isEmpty()) {
            logger.info("No local model assets are configured for automatic startup download")
            return
        }

        logger.info("Checking {} local model asset(s) configured for automatic startup download", assets.size)
        ModelAssetDownloader().downloadMissing(assets)
    }
}

private fun Binder.embeddingModelRegistry(): EmbeddingModelRegistry =
    EmbeddingModelRegistry(bindOrCreate("aisme.embedding", EmbeddingProperties::class.java))

private fun Binder.chatModelRegistry(): ChatModelRegistry =
    ChatModelRegistry(bindOrCreate("aisme.chat", ChatModelsProperties::class.java))

private fun downloadableAssets(
    embeddingModelRegistry: EmbeddingModelRegistry,
    chatModelRegistry: ChatModelRegistry,
): List<ModelAsset> =
    embeddingModelRegistry.embeddingModels().flatMap { model -> model.downloadableAssets() } +
        chatModelRegistry.chatModels().flatMap { model -> model.downloadableAssets() }
