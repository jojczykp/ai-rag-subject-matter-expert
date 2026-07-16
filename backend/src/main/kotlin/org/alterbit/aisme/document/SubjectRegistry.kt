package org.alterbit.aisme.document

interface SubjectRegistry {
    fun subjects(): List<SubjectDescriptor>

    fun getByIdOrThrow(subjectId: String): SubjectDescriptor
}
