package org.alterbit.aisme

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AismeApplicationTest {
    @Test
    fun `application class is available`() {
        AismeApplication::class.simpleName shouldBe "AismeApplication"
    }
}
