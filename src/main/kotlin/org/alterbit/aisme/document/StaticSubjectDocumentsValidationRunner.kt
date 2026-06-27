package org.alterbit.aisme.document

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("no-db")
class StaticSubjectDocumentsValidationRunner(
    private val loader: StaticSubjectDocumentsLoader,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        loader.load()
    }
}
