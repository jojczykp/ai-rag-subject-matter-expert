package org.alterbit.aisme.chat.api

import org.alterbit.aisme.chat.AiChatService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatController(
    private val aiChatService: AiChatService,
) {
    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequestDto): ChatResponseDto =
        aiChatService.chat(request)
}
