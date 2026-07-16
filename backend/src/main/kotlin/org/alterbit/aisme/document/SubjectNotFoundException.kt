package org.alterbit.aisme.document

class SubjectNotFoundException(
    val subjectId: String,
) : RuntimeException("Subject '$subjectId' was not found")
