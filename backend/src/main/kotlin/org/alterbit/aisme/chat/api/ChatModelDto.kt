package org.alterbit.aisme.chat.api

import org.alterbit.aisme.chat.catalog.ChatModelAvailability
import org.alterbit.aisme.chat.catalog.ChatModelCapability
import org.alterbit.aisme.chat.catalog.ChatModelDescriptor
import org.alterbit.aisme.chat.catalog.ChatModelMode
import org.alterbit.aisme.chat.catalog.ChatModelRuntime
import org.alterbit.aisme.chat.catalog.ChatModelRuntimeRequirement

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
