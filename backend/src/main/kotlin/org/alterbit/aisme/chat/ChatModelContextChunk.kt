package org.alterbit.aisme.chat

data class ChatModelContextChunk(
    val content: String,
    val resourcePath: String,
    val chunkIndex: Int,
) {
    init {
        require(content.isNotBlank()) { "content must not be blank" }
        require(resourcePath.isNotBlank()) { "resourcePath must not be blank" }
        require(chunkIndex >= 0) { "chunkIndex must not be negative" }
    }
}
