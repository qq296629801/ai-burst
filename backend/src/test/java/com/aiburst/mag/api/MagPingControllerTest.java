package com.aiburst.mag.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 使用 standalone MockMvc，避免 {@code @WebMvcTest} 拉起含 Jwt 过滤器的完整应用上下文。
 */
class MagPingControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MagPingController())
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();

    @Test
    void pingReturnsOk() throws Exception {
        mockMvc.perform(get("/api/mag/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("mag"));
    }
}
