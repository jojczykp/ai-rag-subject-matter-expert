package org.alterbit.aisme.document

class SubjectDocumentsException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
