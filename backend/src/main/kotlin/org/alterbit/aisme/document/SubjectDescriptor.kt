package org.alterbit.aisme.document

data class SubjectDescriptor(
    val id: String,
    val enabled: Boolean,
    val displayOrder: Int,
    val displayName: String,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(displayName.isNotBlank()) { "displayName must not be blank" }
    }
}
