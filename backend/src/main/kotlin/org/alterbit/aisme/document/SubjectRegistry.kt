package org.alterbit.aisme.document

interface SubjectRegistry {
    fun subjects(): List<SubjectDescriptor>

    fun defaultSubjectId(): String? =
        subjects().firstOrNull()?.id

    fun getByIdOrThrow(subjectId: String): SubjectDescriptor
}
