package org.alterbit.aisme

import org.alterbit.aisme.persistence.SourceDocumentRepository
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan
@EnableJdbcRepositories(basePackageClasses = [SourceDocumentRepository::class])
@ComponentScan(
    basePackages = [
        "org.alterbit.aisme.persistence",
        "org.alterbit.aisme.retrieval",
    ],
)
class DatabaseTestContext
