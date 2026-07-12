package org.alterbit.aisme

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ClockConfiguration {
    @Bean
    fun clock(): Clock =
        Clock.systemUTC()
}
