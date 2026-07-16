package org.alterbit.aisme.document

import org.springframework.stereotype.Service

@Service
class SubjectService(
    private val subjectsProperties: SubjectsProperties,
) : SubjectRegistry {
    override fun subjects(): List<SubjectDescriptor> =
        subjectsProperties.enabledSubjects()

    override fun getByIdOrThrow(subjectId: String): SubjectDescriptor =
        subjectsProperties.getByIdOrThrow(subjectId)
}
