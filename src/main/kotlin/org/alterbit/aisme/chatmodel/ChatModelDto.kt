package org.alterbit.aisme.chatmodel

data class ChatModelDto(
    val id: String,
    val displayName: String,
    val runtime: ChatModelRuntime,
    val mode: ChatModelMode,
    val availability: ChatModelAvailability,
    val availableOffline: Boolean,
    val promptsMayLeaveLocalMachine: Boolean,
)

fun ChatModelDescriptor.toDto(): ChatModelDto =
    ChatModelDto(
        id = id,
        displayName = displayName,
        runtime = runtime,
        mode = mode,
        availability = availability,
        availableOffline = availableOffline,
        promptsMayLeaveLocalMachine = mode == ChatModelMode.ONLINE,
    )
