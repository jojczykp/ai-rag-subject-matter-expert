package org.alterbit.aisme.modelcatalog

data class ChatModelDto(
    val id: String,
    val displayName: String,
    val description: String?,
    val runtime: ChatModelRuntime,
    val mode: ChatModelMode,
    val availability: ChatModelAvailability,
    val availableOffline: Boolean,
    val promptsMayLeaveLocalMachine: Boolean,
    val capabilities: List<ChatModelCapability>,
    val runtimeRequirements: List<ChatModelRuntimeRequirement>,
)

fun ChatModelDescriptor.toDto(): ChatModelDto =
    ChatModelDto(
        id = id,
        displayName = displayName,
        description = description,
        runtime = runtime,
        mode = mode,
        availability = availability,
        availableOffline = availableOffline,
        promptsMayLeaveLocalMachine = mode == ChatModelMode.ONLINE,
        capabilities = listOf(ChatModelCapability.CHAT),
        runtimeRequirements = runtime.runtimeRequirements,
    )
