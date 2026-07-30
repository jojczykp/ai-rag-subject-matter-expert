package org.alterbit.aisme.document.api

import org.alterbit.aisme.document.SubjectDescriptor

data class SubjectDto(
    val id: String,
    val enabled: Boolean,
    val displayOrder: Int,
    val displayName: String,
    val defaultQuestion: String,
)

fun SubjectDescriptor.toDto(): SubjectDto =
    SubjectDto(
        id = id,
        enabled = enabled,
        displayOrder = displayOrder,
        displayName = displayName,
        defaultQuestion = defaultQuestion,
    )
