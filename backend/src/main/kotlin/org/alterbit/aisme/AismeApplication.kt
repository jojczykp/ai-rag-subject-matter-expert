package org.alterbit.aisme

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AismeApplication

fun main(args: Array<String>) {
    runApplication<AismeApplication>(*args)
}
