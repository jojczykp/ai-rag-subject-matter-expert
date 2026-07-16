package org.alterbit.aisme.document.api

data class SubjectsResponseDto(
    val defaultSubjectId: String?,
    val subjects: List<SubjectDto>,
)
