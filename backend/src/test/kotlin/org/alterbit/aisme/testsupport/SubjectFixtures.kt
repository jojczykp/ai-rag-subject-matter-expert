package org.alterbit.aisme.testsupport

import org.alterbit.aisme.document.SubjectDescriptor
import org.alterbit.aisme.document.SubjectDocumentsProperties
import org.alterbit.aisme.document.SubjectProperties
import org.alterbit.aisme.document.SubjectsProperties

const val CULINARY_SUBJECT_ID = "culinary-expert"
const val CULINARY_SUBJECT_DISPLAY_NAME = "Culinary Expert"
const val CULINARY_SUBJECT_DEFAULT_QUESTION = "How should I cook rice?"

fun culinarySubject(
    enabled: Boolean = true,
    displayOrder: Int = 10,
    displayName: String = CULINARY_SUBJECT_DISPLAY_NAME,
    defaultQuestion: String = CULINARY_SUBJECT_DEFAULT_QUESTION,
): SubjectDescriptor =
    SubjectDescriptor(
        id = CULINARY_SUBJECT_ID,
        enabled = enabled,
        displayOrder = displayOrder,
        displayName = displayName,
        defaultQuestion = defaultQuestion,
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
                defaultQuestion = CULINARY_SUBJECT_DEFAULT_QUESTION,
                documents = documentsProperties,
            ),
        ),
    )
