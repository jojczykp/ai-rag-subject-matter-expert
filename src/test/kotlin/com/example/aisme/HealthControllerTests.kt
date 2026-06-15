package com.example.aisme

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `root endpoint returns service status`() {
        mockMvc.get("/")
            .andExpect {
                status { isOk() }
                jsonPath("$.message") {
                    value("AI Subject Matter Expert service is running")
                }
            }
    }
}
