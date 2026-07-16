package org.alterbit.aisme.document

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme")
data class SubjectsProperties(
    val subjects: Map<String, SubjectProperties> = mapOf(
            "culinary-expert" to SubjectProperties(
            displayOrder = 10,
            displayName = "Culinary Expert",
            documents = SubjectDocumentsProperties(
                location = "classpath:/subject_documents/culinary_expert/",
            ),
        ),
    ),
) {
    init {
        require(subjects.isNotEmpty()) { "aisme.subjects must contain at least one subject" }
        subjects.forEach { (subjectId, subject) ->
            require(subjectId.isNotBlank()) { "aisme.subjects keys must not be blank" }
            require(subject.displayName == null || subject.displayName.isNotBlank()) {
                "aisme.subjects.$subjectId.display-name must not be blank"
            }
        }
    }

    fun allSubjects(): List<SubjectDescriptor> =
        subjects
            .map { (subjectId, subject) -> subject.toDescriptor(subjectId) }
            .sortedWith(compareBy(SubjectDescriptor::displayOrder, SubjectDescriptor::id))

    fun enabledSubjects(): List<SubjectDescriptor> =
        allSubjects().filter(SubjectDescriptor::enabled)

    fun getByIdOrThrow(subjectId: String): SubjectDescriptor =
        enabledSubjects().firstOrNull { subject -> subject.id == subjectId }
            ?: throw SubjectNotFoundException(subjectId)

    fun documentsForSubjectOrThrow(subjectId: String): SubjectDocumentsProperties =
        subjects[subjectId]?.takeIf(SubjectProperties::enabled)?.documents
            ?: throw SubjectNotFoundException(subjectId)

    private fun SubjectProperties.toDescriptor(subjectId: String): SubjectDescriptor =
        SubjectDescriptor(
            id = subjectId,
            enabled = enabled,
            displayOrder = displayOrder,
            displayName = displayName ?: subjectId.toDisplayName(),
        )

    private fun String.toDisplayName(): String =
        split('_', '-')
            .filter(String::isNotBlank)
            .joinToString(" ") { segment ->
                segment.replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase()
                    } else {
                        char.toString()
                    }
                }
            }
}
