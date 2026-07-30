package org.alterbit.aisme.document

data class SubjectProperties(
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val displayName: String? = null,
    val defaultQuestion: String = "",
    val documents: SubjectDocumentsProperties = SubjectDocumentsProperties(),
)
