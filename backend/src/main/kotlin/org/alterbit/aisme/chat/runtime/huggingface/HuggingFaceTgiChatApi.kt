package org.alterbit.aisme.chat.runtime.huggingface

import com.fasterxml.jackson.annotation.JsonProperty

interface HuggingFaceTgiChatApi {
    fun generate(request: HuggingFaceTgiGenerateRequest): HuggingFaceTgiGenerateResponse
}

data class HuggingFaceTgiGenerateRequest(
    val inputs: String,
)

data class HuggingFaceTgiGenerateResponse(
    @JsonProperty("generated_text")
    val generatedText: String? = null,
)
