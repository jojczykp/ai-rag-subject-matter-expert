package org.alterbit.aisme.chat

fun interface AiModelClientProvider {
    fun clients(): List<AiModelClient>
}
