package org.alterbit.aisme.embedding.catalog

import java.time.Duration

interface EmbeddingModelAvailabilityChecker {
    fun supports(model: EmbeddingModelDescriptor): Boolean

    fun check(model: EmbeddingModelDescriptor, apiTimeout: Duration): EmbeddingModelAvailability
}
