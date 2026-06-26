package org.alterbit.aisme

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("no-db")
@SpringBootTest
class AismeApplicationTest {
    @Test
    fun contextLoads() {
    }
}
