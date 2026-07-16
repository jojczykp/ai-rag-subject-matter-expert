package org.alterbit.aisme.document

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisme.subjects")
data class SubjectsProperties(
    val defaultSubjectId: String? = "culinary-expert",
    val definitions: Map<String, SubjectProperties> = mapOf(
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
        require(definitions.isNotEmpty()) { "aisme.subjects.definitions must contain at least one subject" }
        require(defaultSubjectId == null || defaultSubjectId.isNotBlank()) {
            "aisme.subjects.default-subject-id must not be blank"
        }
        definitions.forEach { (subjectId, subject) ->
            require(subjectId.isNotBlank()) { "aisme.subjects.definitions keys must not be blank" }
            require(subject.displayName == null || subject.displayName.isNotBlank()) {
                "aisme.subjects.definitions.$subjectId.display-name must not be blank"
            }
        }
        require(defaultSubjectId == null || defaultSubjectId in enabledSubjectIds()) {
            "aisme.subjects.default-subject-id must reference an enabled subject"
        }
    }

    fun allSubjects(): List<SubjectDescriptor> =
        definitions
            .map { (subjectId, subject) -> subject.toDescriptor(subjectId) }
            .sortedWith(compareBy(SubjectDescriptor::displayOrder, SubjectDescriptor::id))

    fun enabledSubjects(): List<SubjectDescriptor> =
        allSubjects().filter(SubjectDescriptor::enabled)

    fun getByIdOrThrow(subjectId: String): SubjectDescriptor =
        enabledSubjects().firstOrNull { subject -> subject.id == subjectId }
            ?: throw SubjectNotFoundException(subjectId)

    fun defaultSubjectId(): String? =
        defaultSubjectId

    fun documentsForSubjectOrThrow(subjectId: String): SubjectDocumentsProperties =
        definitions[subjectId]?.takeIf(SubjectProperties::enabled)?.documents
            ?: throw SubjectNotFoundException(subjectId)

    private fun enabledSubjectIds(): Set<String> =
        definitions
            .filterValues(SubjectProperties::enabled)
            .keys

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
