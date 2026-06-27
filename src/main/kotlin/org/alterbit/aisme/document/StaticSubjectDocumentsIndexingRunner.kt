package org.alterbit.aisme.document

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!no-db & !skip-startup-indexing")
class StaticSubjectDocumentsIndexingRunner(
    private val loader: StaticSubjectDocumentsLoader,
    private val indexer: SubjectDocumentIndexer,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        indexer.index(loader.load())
    }
}
