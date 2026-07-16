package org.alterbit.aisme.chat.api

import org.alterbit.aisme.chat.ChatService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequestDto): ChatResponseDto =
        chatService.chat(request)
}
