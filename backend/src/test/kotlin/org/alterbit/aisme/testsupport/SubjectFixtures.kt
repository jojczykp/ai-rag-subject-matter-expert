package org.alterbit.aisme.testsupport

import org.alterbit.aisme.document.SubjectDescriptor
import org.alterbit.aisme.document.SubjectDocumentsProperties
import org.alterbit.aisme.document.SubjectProperties
import org.alterbit.aisme.document.SubjectsProperties

const val CULINARY_SUBJECT_ID = "culinary-expert"
const val CULINARY_SUBJECT_DISPLAY_NAME = "Culinary Expert"

fun culinarySubject(
    enabled: Boolean = true,
    displayOrder: Int = 10,
    displayName: String = CULINARY_SUBJECT_DISPLAY_NAME,
): SubjectDescriptor =
    SubjectDescriptor(
        id = CULINARY_SUBJECT_ID,
        enabled = enabled,
        displayOrder = displayOrder,
        displayName = displayName,
    )

fun subjectsProperties(
    subjectId: String = CULINARY_SUBJECT_ID,
    documentsProperties: SubjectDocumentsProperties = SubjectDocumentsProperties(),
): SubjectsProperties =
    SubjectsProperties(
        defaultSubjectId = subjectId,
        definitions = mapOf(
            subjectId to SubjectProperties(
                enabled = true,
                displayOrder = 10,
                displayName = CULINARY_SUBJECT_DISPLAY_NAME,
                documents = documentsProperties,
            ),
        ),
    )
