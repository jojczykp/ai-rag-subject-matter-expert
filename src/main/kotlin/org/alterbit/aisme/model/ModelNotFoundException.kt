package org.alterbit.aisme.model

class ModelNotFoundException(modelId: String) : RuntimeException("Configured model not found: $modelId")
