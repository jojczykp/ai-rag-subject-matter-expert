package org.alterbit.aisme

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@ActiveProfiles("no-db")
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorEndpointsTest(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `actuator health endpoint is available`() {
        mockMvc.get("/actuator/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") {
                    value("UP")
                }
            }
    }

    @Test
    fun `actuator info endpoint is available`() {
        mockMvc.get("/actuator/info")
            .andExpect {
                status { isOk() }
            }
    }
}
