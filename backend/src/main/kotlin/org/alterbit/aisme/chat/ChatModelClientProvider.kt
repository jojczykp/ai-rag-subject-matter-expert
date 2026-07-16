package org.alterbit.aisme.chat

fun interface ChatModelClientProvider {
    fun clients(): List<ChatModelClient>
}
